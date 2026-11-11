package com.placewise.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * One student's academic record. Immutable: to "change" a student you build a new one with a
 * {@code withXxx} method, which is what makes it safe to share between threads and to use in the
 * what-if simulator without touching real data.
 *
 * <p>The compact constructor <em>normalizes</em> (trims, upper-cases roll number and branch, gives CGPA
 * two decimals) but does not validate ranges; {@code StudentValidator} does that so it can report every
 * problem at once. The natural ordering ({@link Comparable}) is by roll number; the ranking order lives
 * in {@code StudentRanking} as a {@link java.util.Comparator}.
 *
 * @param rollNo         unique key, stored upper-case (MySQL compares keys case-insensitively)
 * @param name           display name
 * @param branch         e.g. {@code CSE}, stored upper-case
 * @param semester       1..8
 * @param cgpa           0.00..10.00 with scale 2
 * @param activeBacklogs number of uncleared failed subjects, never negative
 * @param email          optional; {@code null} when absent
 */
public record Student(String rollNo, String name, String branch, int semester, BigDecimal cgpa,
                      int activeBacklogs, String email) implements Comparable<Student> {

    /** Normalizes all text fields and the CGPA scale. */
    public Student {
        rollNo = normalizeRoll(rollNo);
        name = clean(name);
        branch = clean(branch).toUpperCase(Locale.ROOT);
        cgpa = Cgpa.normalize(cgpa);
        String trimmedEmail = clean(email);
        email = trimmedEmail.isEmpty() ? null : trimmedEmail;
    }

    /**
     * Brings a roll number into canonical form so lookups ignore case and stray spaces.
     *
     * @param rollNo the text typed by the user (may be {@code null})
     * @return trimmed, upper-case text ({@code ""} for {@code null})
     */
    public static String normalizeRoll(String rollNo) {
        return clean(rollNo).toUpperCase(Locale.ROOT);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * Returns the email as an {@link Optional}, which is clearer than a nullable accessor.
     *
     * @return the email, or empty when none was given
     */
    public Optional<String> optionalEmail() {
        return Optional.ofNullable(email);
    }

    /**
     * Builds a copy with a different CGPA.
     *
     * @param newCgpa the new value
     * @return a new student; this one is unchanged
     */
    public Student withCgpa(BigDecimal newCgpa) {
        return new Student(rollNo, name, branch, semester, newCgpa, activeBacklogs, email);
    }

    /**
     * Builds a copy with a different backlog count.
     *
     * @param backlogs the new count
     * @return a new student; this one is unchanged
     */
    public Student withActiveBacklogs(int backlogs) {
        return new Student(rollNo, name, branch, semester, cgpa, backlogs, email);
    }

    /**
     * Natural order: by roll number.
     *
     * @param other the student to compare with
     * @return negative, zero or positive as usual for {@link Comparable}
     */
    @Override
    public int compareTo(Student other) {
        return rollNo.compareTo(other.rollNo);
    }
}
