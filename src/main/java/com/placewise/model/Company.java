package com.placewise.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A recruiting company and its eligibility criteria. Immutable.
 *
 * @param id              database id; {@code 0} means "not saved yet"
 * @param name            company name
 * @param minCgpa         minimum CGPA, 0.00..10.00
 * @param maxBacklogs     maximum active backlogs allowed, never negative
 * @param allowedBranches upper-case branch codes; an <em>empty</em> set means every branch is allowed
 * @param packageLpa      offered package in lakhs per annum, scale 2
 * @param seats           number of offers the company will make, positive
 */
public record Company(long id, String name, BigDecimal minCgpa, int maxBacklogs,
                      Set<String> allowedBranches, BigDecimal packageLpa, int seats) {

    /** Normalizes the name, CGPA scale, package scale, and turns the branch set into a sorted read-only set. */
    public Company {
        name = name == null ? "" : name.trim();
        minCgpa = Cgpa.normalize(minCgpa);
        allowedBranches = normalizeBranches(allowedBranches);
        packageLpa = packageLpa == null
                ? new BigDecimal("0.00")
                : packageLpa.setScale(2, RoundingMode.HALF_UP);
    }

    private static Set<String> normalizeBranches(Set<String> branches) {
        if (branches == null) {
            return Collections.emptySet();
        }
        // A TreeSet keeps the branches sorted, so they are always printed and saved in the same order.
        TreeSet<String> sorted = new TreeSet<>();
        for (String branch : branches) {
            if (branch != null && !branch.isBlank()) {
                sorted.add(branch.trim().toUpperCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(sorted);
    }

    /**
     * Tells whether students of a branch may apply.
     *
     * @param branch the student's branch code (case-insensitive)
     * @return {@code true} if the allowed set is empty (all branches) or contains the branch
     */
    public boolean allowsBranch(String branch) {
        return allowedBranches.isEmpty()
                || allowedBranches.contains(branch.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Describes the branch restriction for display.
     *
     * @return {@code "ALL"} or a comma-separated list such as {@code "CSE, IT"}
     */
    public String branchesLabel() {
        return allowedBranches.isEmpty() ? "ALL" : String.join(", ", allowedBranches);
    }

    /**
     * Builds a copy carrying a database-generated id.
     *
     * @param newId the id
     * @return a new company; this one is unchanged
     */
    public Company withId(long newId) {
        return new Company(newId, name, minCgpa, maxBacklogs, allowedBranches, packageLpa, seats);
    }

    /**
     * Builds a copy with a different branch list (used when a repository loads the branch rows separately).
     *
     * @param branches the new allowed branches, empty for "all"
     * @return a new company; this one is unchanged
     */
    public Company withAllowedBranches(Set<String> branches) {
        return new Company(id, name, minCgpa, maxBacklogs, branches, packageLpa, seats);
    }
}
