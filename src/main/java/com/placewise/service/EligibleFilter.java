package com.placewise.service;

import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Optional narrowing of an eligible list. A {@code null} field means "no restriction on that".
 *
 * @param branch  keep only this branch (case-insensitive), or {@code null}
 * @param minCgpa keep only students with at least this CGPA, or {@code null}
 */
public record EligibleFilter(String branch, BigDecimal minCgpa) {

    /** Upper-cases the branch so matching ignores case; blank means no branch filter. */
    public EligibleFilter {
        branch = branch == null || branch.isBlank() ? null : branch.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * A filter that keeps everyone.
     *
     * @return the empty filter
     */
    public static EligibleFilter none() {
        return new EligibleFilter(null, null);
    }

    /** True when the student passes both restrictions (a {@code null} restriction lets everyone pass). */
    public boolean matches(Student student) {
        boolean branchOk = branch == null || student.branch().equals(branch);
        boolean cgpaOk = minCgpa == null || student.cgpa().compareTo(minCgpa) >= 0;
        return branchOk && cgpaOk;
    }
}
