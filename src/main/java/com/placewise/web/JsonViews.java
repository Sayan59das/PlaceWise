package com.placewise.web;

import com.placewise.analytics.BranchSummary;
import com.placewise.analytics.Explanation;
import com.placewise.analytics.PlacementSummary;
import com.placewise.concurrency.race.RaceReport;
import com.placewise.drive.BoardSnapshot;
import com.placewise.drive.DriveResult;
import com.placewise.eligibility.EligibilityResult;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.RankedStudent;
import com.placewise.report.Table;
import com.placewise.service.DriveOutcome;
import com.placewise.service.ImportSummary;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns domain objects into maps and lists that {@link Json} can write: the web twin of
 * {@code ui.Views}, which turns the same objects into text tables for the terminal.
 *
 * <p>A {@link LinkedHashMap} keeps the keys in the order they are put, so the JSON reads in a sensible order.
 */
public final class JsonViews {

    private JsonViews() {
    }

    /** One student, every field. */
    public static Map<String, Object> student(Student student) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("rollNo", student.rollNo());
        json.put("name", student.name());
        json.put("branch", student.branch());
        json.put("semester", student.semester());
        json.put("cgpa", student.cgpa());
        json.put("activeBacklogs", student.activeBacklogs());
        json.put("email", student.email());
        return json;
    }

    /** A list of students. */
    public static List<Map<String, Object>> students(List<Student> students) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (Student student : students) {
            json.add(student(student));
        }
        return json;
    }

    /** One company and its criteria; {@code branches} is empty when every branch is allowed. */
    public static Map<String, Object> company(Company company) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", company.id());
        json.put("name", company.name());
        json.put("minCgpa", company.minCgpa());
        json.put("maxBacklogs", company.maxBacklogs());
        json.put("branches", company.allowedBranches());
        json.put("branchesLabel", company.branchesLabel());
        json.put("packageLpa", company.packageLpa());
        json.put("seats", company.seats());
        return json;
    }

    /** A list of companies. */
    public static List<Map<String, Object>> companies(List<Company> companies) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (Company company : companies) {
            json.add(company(company));
        }
        return json;
    }

    /** Just the names, for short lists such as "newly unlocked". */
    public static List<String> names(List<Company> companies) {
        List<String> names = new ArrayList<>();
        for (Company company : companies) {
            names.add(company.name());
        }
        return names;
    }

    /**
     * One eligibility verdict. The verdict comes from the checker; the explanation adds which criteria
     * failed (so the page can mark them) and the exact gap as one sentence.
     */
    public static Map<String, Object> verdict(EligibilityResult result, Explanation explanation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("student", student(result.student()));
        json.put("company", company(result.company()));
        json.put("eligible", result.eligible());
        json.put("cgpaFails", explanation.cgpaShortfall().signum() > 0);
        json.put("backlogsFail", explanation.backlogExcess() > 0);
        json.put("branchFails", explanation.branchBlocked());
        json.put("remark", explanation.summary());
        return json;
    }

    /** Why one company rejects a student: the exact gap on every criterion. */
    public static Map<String, Object> explanation(Explanation explanation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("company", company(explanation.company()));
        json.put("cgpaShortfall", explanation.cgpaShortfall());
        json.put("backlogExcess", explanation.backlogExcess());
        json.put("branchBlocked", explanation.branchBlocked());
        json.put("needs", explanation.needs());
        json.put("summary", explanation.summary());
        return json;
    }

    /** A ranked list: each entry is a rank plus the student. */
    public static List<Map<String, Object>> ranked(List<RankedStudent> ranked) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (RankedStudent entry : ranked) {
            Map<String, Object> row = student(entry.student());
            row.put("rank", entry.rank());
            json.add(row);
        }
        return json;
    }

    /** A report table exactly as the text and CSV exports see it. */
    public static Map<String, Object> table(Table table) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", table.title());
        json.put("headers", table.headers());
        json.put("rows", table.rows());
        return json;
    }

    /** The overall placement numbers. */
    public static Map<String, Object> summary(PlacementSummary summary) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("totalStudents", summary.totalStudents());
        json.put("totalCompanies", summary.totalCompanies());
        json.put("totalSeats", summary.totalSeats());
        json.put("eligibleForAny", summary.eligibleForAny());
        json.put("eligibleForNone", summary.eligibleForNone());
        json.put("avgEligibleCompanies", summary.avgEligibleCompanies());
        json.put("applications", summary.applications());
        json.put("selected", summary.selected());
        json.put("waitlisted", summary.waitlisted());
        json.put("placedStudents", summary.placedStudents());
        json.put("placementPercent", summary.placementPercent());
        return json;
    }

    /** One entry per branch. */
    public static List<Map<String, Object>> branches(List<BranchSummary> summaries) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (BranchSummary summary : summaries) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("branch", summary.branch());
            row.put("count", summary.count());
            row.put("averageCgpa", summary.averageCgpa());
            row.put("highestCgpa", summary.highestCgpa());
            json.add(row);
        }
        return json;
    }

    /** A finished placement drive: one board per company, the totals, and the invariant verdict. */
    public static Map<String, Object> drive(DriveOutcome outcome, List<String> progress) {
        DriveResult result = outcome.result();
        List<Map<String, Object>> boards = new ArrayList<>();
        for (BoardSnapshot board : result.boards()) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("company", board.company().name());
            json.put("seats", board.company().seats());
            json.put("selected", applicants(board.selected()));
            json.put("waitlisted", applicants(board.waitlisted()));
            boards.add(json);
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("boards", boards);
        json.put("applications", result.applications().size());
        json.put("selected", result.count(ApplicationStatus.SELECTED));
        json.put("waitlisted", result.count(ApplicationStatus.WAITLISTED));
        json.put("rejected", result.count(ApplicationStatus.REJECTED));
        json.put("withdrawn", result.count(ApplicationStatus.WITHDRAWN));
        json.put("placedStudents", result.placedStudents());
        json.put("recruiters", result.config().recruiters());
        json.put("elapsedMs", result.elapsed().toMillis());
        json.put("violations", outcome.violations());
        json.put("persistence", outcome.persistence());
        json.put("progress", progress);
        return json;
    }

    private static List<Map<String, Object>> applicants(List<Application> applications) {
        List<Map<String, Object>> json = new ArrayList<>();
        for (Application application : applications) {
            json.add(student(application.student()));
        }
        return json;
    }

    /** The race-condition demo: the same workload against the unsafe and the safe allocator. */
    public static Map<String, Object> race(RaceReport report, int runs) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("threads", report.config().threads());
        json.put("attempts", report.config().threads() * report.config().attemptsPerThread());
        json.put("totalSeats", report.config().totalSeats());
        json.put("unsafeGranted", report.unsafeGranted());
        json.put("unsafeOversold", report.unsafeOversold());
        json.put("safeGranted", report.safeGranted());
        json.put("safeOversold", report.safeOversold());
        json.put("runs", runs);
        json.put("lines", report.lines());
        return json;
    }

    /** What a CSV import did, with every refused row read back from rejects.csv. */
    public static Map<String, Object> importResult(ImportSummary summary, List<List<String>> rejectedRows) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("message", summary.describe());
        json.put("dataRows", summary.dataRows());
        json.put("imported", summary.imported());
        json.put("rejected", summary.rejected());
        json.put("rejectsFile", summary.rejectsFile());
        json.put("rejectedRows", rejectedRows);
        return json;
    }

    /** File paths as text. */
    public static List<String> paths(List<Path> paths) {
        List<String> json = new ArrayList<>();
        for (Path path : paths) {
            json.add(path.toString());
        }
        return json;
    }
}
