package com.placewise.ui;

import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.RankedStudent;
import com.placewise.report.Table;
import com.placewise.report.TextReportRenderer;
import java.util.ArrayList;
import java.util.List;

/** Turns domain objects into aligned text tables by reusing the report renderer. */
public final class Views {

    private static final TextReportRenderer RENDERER = new TextReportRenderer();
    private static final List<String> STUDENT_HEADERS =
            List.of("Roll No", "Name", "Branch", "Sem", "CGPA", "Backlogs", "Email");

    private Views() {
    }

    public static String students(String title, List<Student> students) {
        List<List<String>> rows = new ArrayList<>();
        for (Student student : students) {
            rows.add(studentCells(student));
        }
        return RENDERER.render(new Table(title, STUDENT_HEADERS, rows));
    }

    public static String ranked(String title, List<RankedStudent> ranked) {
        List<String> headers = new ArrayList<>();
        headers.add("Rank");
        headers.addAll(STUDENT_HEADERS);
        List<List<String>> rows = new ArrayList<>();
        for (RankedStudent rankedStudent : ranked) {
            List<String> cells = new ArrayList<>();
            cells.add(String.valueOf(rankedStudent.rank()));
            cells.addAll(studentCells(rankedStudent.student()));
            rows.add(cells);
        }
        return RENDERER.render(new Table(title, headers, rows));
    }

    public static String companies(List<Company> companies) {
        List<List<String>> rows = new ArrayList<>();
        for (Company c : companies) {
            rows.add(List.of(String.valueOf(c.id()), c.name(), c.minCgpa().toPlainString(),
                    String.valueOf(c.maxBacklogs()), c.branchesLabel(), c.packageLpa().toPlainString(),
                    String.valueOf(c.seats())));
        }
        return RENDERER.render(new Table("Companies",
                List.of("Id", "Name", "Min CGPA", "Max backlogs", "Branches", "Package LPA", "Seats"), rows));
    }

    private static List<String> studentCells(Student s) {
        return List.of(s.rollNo(), s.name(), s.branch(), String.valueOf(s.semester()), s.cgpa().toPlainString(),
                String.valueOf(s.activeBacklogs()), s.email() == null ? "" : s.email());
    }
}
