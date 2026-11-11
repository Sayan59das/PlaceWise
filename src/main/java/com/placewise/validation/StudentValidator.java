package com.placewise.validation;

import com.placewise.exception.InvalidStudentException;
import com.placewise.model.Cgpa;
import com.placewise.model.Student;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Checks a {@link Student} and reports every problem at once, so the user can fix them all in one go. */
public final class StudentValidator {

    /** Deliberately simple: something@something.something with no spaces. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private StudentValidator() {
    }

    /** Returns every rule the student breaks; an empty list means the student is valid. */
    public static List<String> problems(Student student) {
        List<String> problems = new ArrayList<>();
        if (student.rollNo().isBlank()) {
            problems.add("Roll number must not be blank");
        }
        if (student.name().isBlank()) {
            problems.add("Name must not be blank");
        }
        if (student.branch().isBlank()) {
            problems.add("Branch must not be blank");
        }
        if (!Range.isBetween(student.semester(), 1, 8)) {
            problems.add("Semester must be between 1 and 8");
        }
        if (!Range.isBetween(student.cgpa(), Cgpa.MIN, Cgpa.MAX)) {
            problems.add("CGPA must be between 0.00 and 10.00");
        }
        if (student.activeBacklogs() < 0) {
            problems.add("Active backlogs must not be negative");
        }
        if (student.email() != null && !EMAIL.matcher(student.email()).matches()) {
            problems.add("Email address is not valid");
        }
        return problems;
    }

    /** Throws {@link InvalidStudentException} (listing all problems) if the student is not valid. */
    public static Student requireValid(Student student) {
        List<String> problems = problems(student);
        if (!problems.isEmpty()) {
            throw new InvalidStudentException(problems);
        }
        return student;
    }
}
