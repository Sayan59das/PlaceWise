package com.placewise.report;

import com.placewise.analytics.BranchSummary;
import com.placewise.analytics.PlacementSummary;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.RankedStudent;
import com.placewise.ranking.StudentSort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the four placement reports as {@link Table}s. It only turns data into rows; it writes no files. */
public final class ReportBuilder {

    private static final List<String> STUDENT_COLUMNS =
            List.of("Roll No", "Name", "Branch", "Semester", "CGPA", "Backlogs");

    private ReportBuilder() {
    }

    /** Eligible students of one company, ordered by roll number. */
    public static Table eligibleList(Company company, List<Student> eligible) {
        List<Student> sorted = new ArrayList<>(eligible);
        sorted.sort(StudentSort.ROLL_NO.comparator());
        List<List<String>> rows = new ArrayList<>();
        for (Student student : sorted) {
            rows.add(studentCells(student));
        }
        return new Table("Eligible students - " + company.name(), STUDENT_COLUMNS, rows);
    }

    /** Ranked eligible students of one company, with a Rank column in front. */
    public static Table rankedList(Company company, List<RankedStudent> ranked) {
        List<String> headers = new ArrayList<>();
        headers.add("Rank");
        headers.addAll(STUDENT_COLUMNS);
        List<List<String>> rows = new ArrayList<>();
        for (RankedStudent rankedStudent : ranked) {
            List<String> cells = new ArrayList<>();
            cells.add(String.valueOf(rankedStudent.rank()));
            cells.addAll(studentCells(rankedStudent.student()));
            rows.add(cells);
        }
        return new Table("Ranked eligible students - " + company.name(), headers, rows);
    }

    /** One row per branch. */
    public static Table branchSummary(List<BranchSummary> summaries) {
        List<List<String>> rows = new ArrayList<>();
        for (BranchSummary s : summaries) {
            rows.add(List.of(s.branch(), String.valueOf(s.count()), s.averageCgpa().toPlainString(),
                    s.highestCgpa().toPlainString()));
        }
        return new Table("Branch-wise summary", List.of("Branch", "Students", "Average CGPA", "Highest CGPA"), rows);
    }

    /** The overall numbers, as Metric / Value rows. */
    public static Table overallSummary(PlacementSummary summary) {
        List<List<String>> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : summary.toRows().entrySet()) {
            rows.add(List.of(entry.getKey(), entry.getValue()));
        }
        return new Table("Overall placement summary", List.of("Metric", "Value"), rows);
    }

    private static List<String> studentCells(Student s) {
        return List.of(s.rollNo(), s.name(), s.branch(), String.valueOf(s.semester()), s.cgpa().toPlainString(),
                String.valueOf(s.activeBacklogs()));
    }
}
