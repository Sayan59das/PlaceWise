package com.placewise.analytics;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Cgpa;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * "If my CGPA rose by X, or I cleared Y backlogs, which companies would open up?"
 *
 * <p>{@link Student} is immutable, so a scenario is a <em>copy</em> of the student; the real data is never
 * changed. The same {@link EligibilityChecker} judges the real and the imagined student.
 */
public final class WhatIfSimulator {

    private final EligibilityChecker checker;

    /** Creates the simulator with the rules to use. */
    public WhatIfSimulator(EligibilityChecker checker) {
        this.checker = checker;
    }

    /** Builds the imagined student: CGPA raised (at most 10.00) and backlogs cleared (at least 0). */
    public Student apply(Student student, BigDecimal cgpaIncrease, int backlogsCleared) {
        if (cgpaIncrease.signum() < 0 || backlogsCleared < 0) {
            throw new IllegalArgumentException("Improvements must not be negative");
        }
        BigDecimal newCgpa = student.cgpa().add(cgpaIncrease).min(Cgpa.MAX);
        int newBacklogs = Math.max(0, student.activeBacklogs() - backlogsCleared);
        return student.withCgpa(newCgpa).withActiveBacklogs(newBacklogs);
    }

    /** Runs the scenario for one student: who is eligible already, who unlocks, who stays blocked. */
    public WhatIfResult simulate(Student student, Collection<Company> companies,
                                 BigDecimal cgpaIncrease, int backlogsCleared) {
        Student scenario = apply(student, cgpaIncrease, backlogsCleared);
        List<Company> alreadyEligible = new ArrayList<>();
        List<Company> newlyUnlocked = new ArrayList<>();
        List<Company> stillBlocked = new ArrayList<>();
        for (Company company : companies) {
            boolean eligibleNow = checker.isEligible(student, company);
            boolean eligibleAfter = checker.isEligible(scenario, company);
            if (eligibleNow) {
                alreadyEligible.add(company);
            }
            if (!eligibleNow && eligibleAfter) {
                newlyUnlocked.add(company);
            }
            if (!eligibleAfter) {
                stillBlocked.add(company);
            }
        }
        return new WhatIfResult(student, scenario, alreadyEligible, newlyUnlocked, stillBlocked);
    }

    /**
     * Runs the same scenario for every student and counts, per company, how many students newly qualify.
     * Every company is in the map (with 0 if nobody unlocks it), sorted by company name.
     */
    public Map<String, Long> cohortUnlocks(Collection<Student> students, Collection<Company> companies,
                                           BigDecimal cgpaIncrease, int backlogsCleared) {
        Map<String, Long> counts = new TreeMap<>();
        for (Company company : companies) {
            counts.put(company.name(), 0L);
        }
        for (Student student : students) {
            WhatIfResult result = simulate(student, companies, cgpaIncrease, backlogsCleared);
            for (Company company : result.newlyUnlocked()) {
                counts.put(company.name(), counts.get(company.name()) + 1);
            }
        }
        return counts;
    }
}
