package com.placewise;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.Set;

/** Small factory methods so tests read as data, not as constructor noise. */
public final class Fixtures {

    private Fixtures() {
    }

    /** A semester-6 student without email. */
    public static Student student(String roll, String name, String branch, String cgpa, int backlogs) {
        return new Student(roll, name, branch, 6, new BigDecimal(cgpa), backlogs, null);
    }

    /** A CSE student named after the roll number. */
    public static Student student(String roll, String cgpa, int backlogs) {
        return student(roll, "Student " + roll, "CSE", cgpa, backlogs);
    }

    /** An unsaved (id 0) company with a 10 LPA package. */
    public static Company company(String name, String minCgpa, int maxBacklogs, int seats, String... branches) {
        return company(0, name, minCgpa, maxBacklogs, seats, branches);
    }

    /** A company with an explicit id. */
    public static Company company(long id, String name, String minCgpa, int maxBacklogs, int seats,
                                  String... branches) {
        return new Company(id, name, new BigDecimal(minCgpa), maxBacklogs, Set.of(branches),
                new BigDecimal("10"), seats);
    }
}
