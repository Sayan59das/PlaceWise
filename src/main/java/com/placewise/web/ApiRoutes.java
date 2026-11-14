package com.placewise.web;

import com.placewise.AppContext;
import com.placewise.analytics.Explanation;
import com.placewise.analytics.StudentAnalytics;
import com.placewise.analytics.WhatIfResult;
import com.placewise.analytics.WhatIfSimulator;
import com.placewise.analytics.WhyNotEligibleExplainer;
import com.placewise.concurrency.ThreadLifecycleDemo;
import com.placewise.concurrency.race.RaceConfig;
import com.placewise.concurrency.race.RaceDemo;
import com.placewise.concurrency.race.RaceReport;
import com.placewise.csv.CsvLine;
import com.placewise.drive.DriveConfig;
import com.placewise.eligibility.EligibilityResult;
import com.placewise.exception.DriveException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.report.Table;
import com.placewise.service.DriveOutcome;
import com.placewise.service.EligibleFilter;
import com.placewise.service.ImportSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every call the web page can make, one method each. A method reads the form, calls the same service the
 * terminal menu calls, and returns the answer as maps and lists ({@link WebServer} turns it into JSON).
 * No business rule lives here: if the terminal and the page disagreed, it would be a bug in this file.
 */
final class ApiRoutes {

    /** The race is statistical: a lucky run may show no oversell, so try a few times (as the terminal does). */
    private static final int RACE_ATTEMPTS = 5;

    private final AppContext app;

    ApiRoutes(AppContext app) {
        this.app = app;
    }

    /**
     * Runs one API call. This table is the whole API at a glance: GET reads, POST changes something.
     *
     * @param route the method and path, for example {@code "GET /api/students"}
     * @param form  the values the page sent
     * @return the answer, or {@code null} when no API call has that route
     */
    Object handle(String route, Form form) {
        return switch (route) {
            case "GET /api/overview" -> overview();
            case "GET /api/students" -> students(form);
            case "POST /api/students/add" -> addStudent(form);
            case "POST /api/students/update" -> updateStudent(form);
            case "POST /api/students/delete" -> deleteStudent(form);
            case "GET /api/companies" -> companies();
            case "POST /api/companies/add" -> addCompany(form);
            case "POST /api/companies/update" -> updateCompany(form);
            case "POST /api/companies/delete" -> deleteCompany(form);
            case "GET /api/eligibility/student" -> eligibilityOfStudent(form);
            case "GET /api/eligibility/company" -> eligibilityForCompany(form);
            case "GET /api/ranked" -> ranked(form);
            case "GET /api/explain" -> explain(form);
            case "GET /api/what-if" -> whatIf(form);
            case "POST /api/drive/generated" -> generatedDrive(form);
            case "POST /api/drive/stored" -> storedDrive(form);
            case "POST /api/drive/race" -> race(form);
            case "POST /api/drive/lifecycle" -> lifecycle();
            case "GET /api/report" -> report(form);
            case "POST /api/export" -> export(form);
            case "POST /api/import/sample" -> importSample(form);
            case "POST /api/import/upload" -> importUpload(form);
            default -> null;
        };
    }

    // ----- Overview -------------------------------------------------------------------------------------

    private Object overview() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("storage", app.storageDescription());
        json.put("summary", JsonViews.summary(app.reports().overallSummary()));
        json.put("branches", JsonViews.branches(
                StudentAnalytics.branchSummaries(app.students().listAll(StudentSort.ROLL_NO))));
        return json;
    }

    // ----- Students (terminal menu 1) -------------------------------------------------------------------

    private Object students(Form form) {
        StudentSort sort = StudentSort.parse(form.text("sort")).orElse(StudentSort.ROLL_NO);
        List<Student> found = new ArrayList<>(app.students().searchByName(form.text("search"))); // blank = all
        found.sort(sort.comparator());
        return Map.of("students", JsonViews.students(found), "sort", sort);
    }

    private Object addStudent(Form form) {
        Student saved = app.students().add(studentFrom(form));
        return message("Added " + saved.rollNo() + " - " + saved.name() + ".");
    }

    private Object updateStudent(Form form) {
        Student saved = app.students().update(studentFrom(form));
        return message("Updated " + saved.rollNo() + ".");
    }

    private Object deleteStudent(Form form) {
        Student student = app.students().get(form.text("rollNo"));
        app.students().delete(student.rollNo());
        return message("Deleted " + student.rollNo() + " - " + student.name() + ".");
    }

    /** The Student record normalizes the values; StudentValidator (inside the service) checks them. */
    private static Student studentFrom(Form form) {
        return new Student(form.text("rollNo"), form.text("name"), form.text("branch"), form.integer("semester"),
                form.decimal("cgpa"), form.integer("activeBacklogs"), form.text("email"));
    }

    // ----- Companies (terminal menu 2) ------------------------------------------------------------------

    private Object companies() {
        return Map.of("companies", JsonViews.companies(app.companies().listAll()));
    }

    private Object addCompany(Form form) {
        Company saved = app.companies().add(companyFrom(form, 0));
        return message("Added company " + saved.id() + " - " + saved.name() + ".");
    }

    private Object updateCompany(Form form) {
        Company saved = app.companies().update(companyFrom(form, form.number("id")));
        return message("Updated company " + saved.id() + " - " + saved.name() + ".");
    }

    private Object deleteCompany(Form form) {
        Company company = app.companies().get(form.number("id"));
        app.companies().delete(company.id());
        return message("Deleted " + company.name() + " (and its applications).");
    }

    /** The id is 0 for a new company (the repository gives it a real one). */
    private static Company companyFrom(Form form, long id) {
        return new Company(id, form.text("name"), form.decimal("minCgpa"), form.integer("maxBacklogs"),
                branchesFrom(form.text("branches")), form.decimal("packageLpa"), form.integer("seats"));
    }

    /** "CSE, IT" becomes {CSE, IT}. Blank or "all" means every branch (an empty set). */
    private static Set<String> branchesFrom(String text) {
        if (text.equalsIgnoreCase("all")) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(text.split("[,;|\\s]+"))); // blank pieces are dropped by Company
    }

    // ----- Eligibility (terminal menu 3) ----------------------------------------------------------------

    private Object eligibilityOfStudent(Form form) {
        String rollNo = form.text("rollNo");
        Student student = app.students().get(rollNo);
        List<EligibilityResult> results = app.eligibility().checkAgainstAllCompanies(rollNo);
        return Map.of("student", JsonViews.student(student), "rows", verdicts(results));
    }

    private Object eligibilityForCompany(Form form) {
        long companyId = form.number("companyId");
        Company company = app.companies().get(companyId);
        List<EligibilityResult> results = app.eligibility().checkAllStudents(companyId);
        return Map.of("company", JsonViews.company(company), "rows", verdicts(results));
    }

    /** Adds to each verdict which criteria failed and by how much, so the page can mark them. */
    private List<Map<String, Object>> verdicts(List<EligibilityResult> results) {
        WhyNotEligibleExplainer explainer = app.explainer();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EligibilityResult result : results) {
            rows.add(JsonViews.verdict(result, explainer.explain(result.student(), result.company())));
        }
        return rows;
    }

    // ----- Ranked lists (terminal menu 4) ---------------------------------------------------------------

    private Object ranked(Form form) {
        long companyId = form.number("companyId");
        EligibleFilter filter = new EligibleFilter(form.text("branch"), form.decimal("minCgpa", null));
        int page = form.integer("page", 1);
        int pageSize = form.integer("pageSize", 10);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("company", JsonViews.company(app.companies().get(companyId)));
        json.put("total", app.listings().eligibleStudents(companyId, filter).size());
        json.put("page", page);
        json.put("pageSize", pageSize);
        json.put("students", JsonViews.ranked(app.listings().page(companyId, filter, page, pageSize)));
        return json;
    }

    // ----- Why not eligible (terminal menu 5) and what-if (terminal menu 6) -----------------------------

    private Object explain(Form form) {
        Student student = app.students().get(form.text("rollNo"));
        List<Company> companies = app.companies().listAll();
        List<Map<String, Object>> rejections = new ArrayList<>();
        for (Explanation explanation : app.explainer().explainRejections(student, companies)) {
            rejections.add(JsonViews.explanation(explanation));
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("student", JsonViews.student(student));
        json.put("companies", companies.size());
        json.put("rejections", rejections);
        return json;
    }

    private Object whatIf(Form form) {
        Student student = app.students().get(form.text("rollNo"));
        BigDecimal increase = form.decimal("cgpaIncrease", BigDecimal.ZERO);
        int cleared = form.integer("backlogsCleared", 0);
        List<Company> companies = app.companies().listAll();
        WhatIfSimulator simulator = app.whatIf();
        WhatIfResult result = simulator.simulate(student, companies, increase, cleared);
        Map<String, Long> unlocks = simulator.cohortUnlocks(app.students().listAll(StudentSort.ROLL_NO), companies,
                increase, cleared);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("student", JsonViews.student(student));
        json.put("scenario", JsonViews.student(result.scenario()));
        json.put("alreadyEligible", JsonViews.names(result.alreadyEligible()));
        json.put("newlyUnlocked", JsonViews.names(result.newlyUnlocked()));
        json.put("stillBlocked", JsonViews.names(result.stillBlocked()));
        json.put("cohortUnlocks", unlocks);
        return json;
    }

    // ----- Placement drive and thread demos (terminal menu 7) -------------------------------------------

    private Object generatedDrive(Form form) {
        DriveConfig defaults = DriveConfig.defaults();
        DriveConfig config = new DriveConfig(form.integer("students", defaults.students()),
                form.integer("companies", defaults.companies()), form.integer("seats", defaults.seatsPerCompany()),
                form.integer("recruiters", defaults.recruiters()), form.number("seed", defaults.seed()),
                defaults.queueCapacity(), defaults.maxApplicationsPerStudent(), defaults.withdrawalPercent(),
                defaults.duplicatePercent());
        // the progress-monitor thread prints into this buffer, so the page can show its lines too
        ByteArrayOutputStream progress = new ByteArrayOutputStream();
        DriveOutcome outcome = app.drives().runGenerated(config, new PrintStream(progress, true, StandardCharsets.UTF_8));
        return JsonViews.drive(outcome, progress.toString(StandardCharsets.UTF_8).lines().toList());
    }

    private Object storedDrive(Form form) {
        DriveConfig defaults = DriveConfig.defaults();
        // as in the terminal: the stored data decides the sizes, so only threads and seed are read
        DriveConfig config = new DriveConfig(1, 1, 1, form.integer("recruiters", defaults.recruiters()),
                form.number("seed", defaults.seed()), defaults.queueCapacity(), defaults.maxApplicationsPerStudent(),
                defaults.withdrawalPercent(), defaults.duplicatePercent());
        ByteArrayOutputStream progress = new ByteArrayOutputStream();
        DriveOutcome outcome = app.drives().runStored(config, new PrintStream(progress, true, StandardCharsets.UTF_8));
        return JsonViews.drive(outcome, progress.toString(StandardCharsets.UTF_8).lines().toList());
    }

    private Object race(Form form) {
        RaceConfig defaults = RaceConfig.defaults();
        RaceConfig config = new RaceConfig(form.integer("threads", defaults.threads()), defaults.companies(),
                form.integer("seats", defaults.seatsPerCompany()), defaults.attemptsPerThread(),
                form.integer("window", defaults.windowMillis()), defaults.seed());
        try {
            RaceReport report = RaceDemo.run(config);
            int runs = 1;
            while (report.unsafeOversold() == 0 && runs < RACE_ATTEMPTS) {
                report = RaceDemo.run(config); // this run was lucky: try again
                runs++;
            }
            return JsonViews.race(report, runs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriveException("The demo was interrupted", e);
        }
    }

    private Object lifecycle() {
        try {
            List<Map<String, String>> states = new ArrayList<>();
            for (Thread.State state : new ThreadLifecycleDemo().run()) {
                states.add(Map.of("state", state.name(), "meaning", ThreadLifecycleDemo.explain(state)));
            }
            return Map.of("states", states);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriveException("The demo was interrupted", e);
        }
    }

    // ----- Reports and export (terminal menu 8) ---------------------------------------------------------

    private Object report(Form form) {
        String name = form.text("name");
        Table table = switch (name) {
            case "overall" -> app.reports().overallTable();
            case "branch" -> app.reports().branchTable();
            case "eligible" -> app.reports().eligibleTable(form.number("companyId"));
            case "ranked" -> app.reports().rankedTable(form.number("companyId"));
            default -> throw new IllegalArgumentException("Unknown report: " + name);
        };
        return JsonViews.table(table);
    }

    /** Files always go into the data folder: the page never chooses a path on this computer. */
    private Object export(Form form) {
        String what = form.text("what");
        List<Path> files = new ArrayList<>();
        switch (what) {
            case "reports" -> files.addAll(app.reports().exportAll(app.dataDir().resolve("reports")));
            case "students" -> {
                Path file = app.dataDir().resolve("students-export.csv");
                app.reports().exportStudents(file);
                files.add(file);
            }
            case "companies" -> {
                Path file = app.dataDir().resolve("companies-export.csv");
                app.reports().exportCompanies(file);
                files.add(file);
            }
            case "audit" -> files.add(app.backupAudit());
            default -> throw new IllegalArgumentException("Unknown export: " + what);
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("message", "Wrote " + files.size() + (files.size() == 1 ? " file" : " files"));
        json.put("files", JsonViews.paths(files));
        return json;
    }

    // ----- CSV import (terminal menu 9) -----------------------------------------------------------------

    /** Only these three sample files can be imported by name. */
    private Object importSample(Form form) {
        String sample = form.text("sample");
        return switch (sample) {
            case "students" -> importStudents(Path.of("sample-data", "students.csv"));
            case "students-with-errors" -> importStudents(Path.of("sample-data", "students-with-errors.csv"));
            case "companies" -> importCompanies(Path.of("sample-data", "companies.csv"));
            default -> throw new IllegalArgumentException("Unknown sample file: " + sample);
        };
    }

    /** The importer reads files, so the uploaded text is saved first, always under one fixed name. */
    private Object importUpload(Form form) {
        String type = form.text("type");
        if (!type.equals("students") && !type.equals("companies")) {
            throw new IllegalArgumentException("Choose whether the file holds students or companies");
        }
        Path file = app.dataDir().resolve("uploaded-" + type + ".csv");
        try {
            Files.writeString(file, form.text("csv"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot save the uploaded file: " + e.getMessage(), e);
        }
        return type.equals("students") ? importStudents(file) : importCompanies(file);
    }

    private Object importStudents(Path file) {
        ImportSummary summary = app.imports().importStudents(file, app.dataDir().resolve("rejects.csv"));
        return JsonViews.importResult(summary, readRejects(summary));
    }

    private Object importCompanies(Path file) {
        ImportSummary summary = app.imports().importCompanies(file, app.dataDir().resolve("rejects.csv"));
        return JsonViews.importResult(summary, readRejects(summary));
    }

    /** Reads rejects.csv back (line, reason, row), so the page shows exactly what the file contains. */
    private static List<List<String>> readRejects(ImportSummary summary) {
        List<List<String>> rows = new ArrayList<>();
        if (summary.rejectsFile() == null) {
            return rows; // nothing was refused, so no file was written
        }
        try {
            List<String> lines = Files.readAllLines(summary.rejectsFile(), StandardCharsets.UTF_8);
            for (String line : lines.subList(1, lines.size())) { // line 0 is the header
                rows.add(CsvLine.parse(line));
            }
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot read " + summary.rejectsFile() + ": " + e.getMessage(), e);
        }
        return rows;
    }

    private static Map<String, Object> message(String text) {
        return Map.of("message", text);
    }
}
