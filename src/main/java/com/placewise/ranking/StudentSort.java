package com.placewise.ranking;

import com.placewise.model.Student;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

/** The fields a student list can be sorted by. Ties are always broken by roll number. */
public enum StudentSort {
    ROLL_NO, NAME, BRANCH, SEMESTER, CGPA, BACKLOGS;

    /** Returns a comparator for this field (a lambda: it compares two students {@code a} and {@code b}). */
    public Comparator<Student> comparator() {
        return (a, b) -> {
            int result = switch (this) {
                case ROLL_NO -> 0;                                          // decided by the tie-break below
                case NAME -> a.name().compareToIgnoreCase(b.name());
                case BRANCH -> a.branch().compareTo(b.branch());
                case SEMESTER -> Integer.compare(a.semester(), b.semester());
                case CGPA -> b.cgpa().compareTo(a.cgpa());                  // highest CGPA first
                case BACKLOGS -> Integer.compare(a.activeBacklogs(), b.activeBacklogs());
            };
            if (result != 0) {
                return result;
            }
            return a.rollNo().compareTo(b.rollNo());
        };
    }

    /** Finds a constant from text typed by the user, for example {@code "cgpa"}; empty if none matches. */
    public static Optional<StudentSort> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String wanted = text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (StudentSort sort : values()) {
            if (sort.name().equals(wanted)) {
                return Optional.of(sort);
            }
        }
        return Optional.empty();
    }
}
