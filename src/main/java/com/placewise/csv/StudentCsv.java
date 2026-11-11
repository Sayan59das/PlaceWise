package com.placewise.csv;

import com.placewise.model.Student;
import com.placewise.validation.StudentValidator;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reads and writes student files: {@code rollNo,name,branch,semester,cgpa,activeBacklogs,email}. */
public final class StudentCsv extends CsvImporter<Student> {

    /** The header every student file must start with. */
    public static final List<String> HEADER =
            List.of("rollNo", "name", "branch", "semester", "cgpa", "activeBacklogs", "email");

    private StudentCsv() {
        super(HEADER);
    }

    /** Creates a reader for student files. */
    public static StudentCsv importer() {
        return new StudentCsv();
    }

    /** Converts one row into a validated student. */
    @Override
    protected Student parseRow(List<String> fields) {
        if (fields.size() != HEADER.size()) {
            throw new IllegalArgumentException("expected " + HEADER.size() + " columns but found " + fields.size());
        }
        Student student = new Student(fields.get(0), fields.get(1), fields.get(2),
                parseInt(fields.get(3), "semester"), parseCgpa(fields.get(4)),
                parseInt(fields.get(5), "activeBacklogs"), fields.get(6));
        return StudentValidator.requireValid(student);
    }

    /** Two rows with the same roll number are duplicates. */
    @Override
    protected String keyOf(Student student) {
        return student.rollNo();
    }

    /** Parses a CGPA carefully: the text comes from a file, so even "1e999999999" must not cause trouble. */
    static BigDecimal parseCgpa(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CGPA '" + text.trim() + "' is not a number");
        }
        // A real CGPA has at most 2 digits before the point; refusing absurd sizes avoids huge numbers.
        if (value.precision() - value.scale() > 6 || value.scale() > 20) {
            throw new IllegalArgumentException("CGPA '" + text.trim() + "' is out of range");
        }
        return value;
    }

    static int parseInt(String text, String column) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " '" + text.trim() + "' is not a whole number");
        }
    }

    /** Turns a student into CSV fields, in header order. */
    public static List<String> toFields(Student student) {
        return List.of(student.rollNo(), student.name(), student.branch(), String.valueOf(student.semester()),
                student.cgpa().toPlainString(), String.valueOf(student.activeBacklogs()),
                student.email() == null ? "" : student.email());
    }

    /** Writes students to a CSV file that {@link #importer()} can read back unchanged. */
    public static void export(Path file, Collection<Student> students) {
        List<List<String>> rows = new ArrayList<>();
        for (Student student : students) {
            rows.add(toFields(student));
        }
        CsvFiles.write(file, HEADER, rows);
    }
}
