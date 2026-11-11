package com.placewise.service;

import com.placewise.analytics.StudentAnalytics;
import com.placewise.csv.CompanyCsv;
import com.placewise.csv.StudentCsv;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.report.CsvReportRenderer;
import com.placewise.report.ReportBuilder;
import com.placewise.report.ReportExporter;
import com.placewise.report.ReportRenderer;
import com.placewise.report.Table;
import com.placewise.report.TextReportRenderer;
import com.placewise.storage.ApplicationRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Builds the placement reports from live data and writes them as text and CSV. */
public final class ReportService {

    /** The two supported formats; the loop over this list is polymorphism in action. */
    public static final List<ReportRenderer> FORMATS = List.of(new TextReportRenderer(), new CsvReportRenderer());

    private final StudentService students;
    private final CompanyService companies;
    private final ListingService listings;
    private final ApplicationRepository applications;
    private final EligibilityChecker checker;

    /**
     * Creates the service.
     *
     * @param students     student lookups
     * @param companies    company lookups
     * @param listings     eligible/ranked lists
     * @param applications stored drive results, used by the overall summary
     * @param checker      the eligibility rules
     */
    public ReportService(StudentService students, CompanyService companies, ListingService listings,
                         ApplicationRepository applications, EligibilityChecker checker) {
        this.students = students;
        this.companies = companies;
        this.listings = listings;
        this.applications = applications;
        this.checker = checker;
    }

    /**
     * Eligible students of a company.
     *
     * @param companyId the company id
     * @return the report data
     */
    public Table eligibleTable(long companyId) {
        Company company = companies.get(companyId);
        return ReportBuilder.eligibleList(company, listings.eligibleStudents(companyId, EligibleFilter.none()));
    }

    /**
     * Ranked eligible students of a company.
     *
     * @param companyId the company id
     * @return the report data
     */
    public Table rankedTable(long companyId) {
        Company company = companies.get(companyId);
        return ReportBuilder.rankedList(company, listings.ranked(companyId, EligibleFilter.none()));
    }

    /**
     * Branch-wise summary of all students.
     *
     * @return the report data
     */
    public Table branchTable() {
        return ReportBuilder.branchSummary(StudentAnalytics.branchSummaries(students.listAll(StudentSort.ROLL_NO)));
    }

    /**
     * Overall placement summary, using the stored results of the latest drive (if any).
     *
     * @return the report data
     */
    public Table overallTable() {
        return ReportBuilder.overallSummary(StudentAnalytics.placementSummary(
                students.listAll(StudentSort.ROLL_NO), companies.listAll(), applications.findAll(), checker));
    }

    /**
     * Writes one report in one format.
     *
     * @param table     the report data
     * @param renderer  the format
     * @param directory target directory
     * @param baseName  file name without extension
     * @return the file written
     */
    public Path export(Table table, ReportRenderer renderer, Path directory, String baseName) {
        return ReportExporter.export(table, renderer, directory, baseName);
    }

    /**
     * Writes every report - per-company eligible and ranked lists, branch summary, overall summary - in
     * both formats.
     *
     * @param directory target directory
     * @return every file written
     */
    public List<Path> exportAll(Path directory) {
        List<Path> written = new ArrayList<>();
        for (ReportRenderer renderer : FORMATS) {
            for (Company company : companies.listAll()) {
                written.add(export(eligibleTable(company.id()), renderer, directory, "eligible-" + company.name()));
                written.add(export(rankedTable(company.id()), renderer, directory, "ranked-" + company.name()));
            }
            written.add(export(branchTable(), renderer, directory, "branch-summary"));
            written.add(export(overallTable(), renderer, directory, "overall-summary"));
        }
        return written;
    }

    /**
     * Writes all students to a CSV file that the importer can read back.
     *
     * @param file target file
     * @return the number of students written
     */
    public int exportStudents(Path file) {
        List<Student> all = students.listAll(StudentSort.ROLL_NO);
        com.placewise.csv.StudentCsv.export(file, all);
        return all.size();
    }

    /**
     * Writes all companies to a CSV file that the importer can read back.
     *
     * @param file target file
     * @return the number of companies written
     */
    public int exportCompanies(Path file) {
        List<Company> all = companies.listAll();
        com.placewise.csv.CompanyCsv.export(file, all);
        return all.size();
    }
}
