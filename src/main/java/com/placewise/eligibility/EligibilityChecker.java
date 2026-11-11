package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runs a list of {@link EligibilityRule}s and collects the reason of <em>every</em> rule that fails.
 *
 * <p>The checker never changes after it is created, so one instance can safely be shared by many threads.
 */
public final class EligibilityChecker {

    private final List<EligibilityRule> rules;

    /** Creates a checker with the three standard rules: CGPA, backlogs and branch. */
    public EligibilityChecker() {
        this(List.of(new CgpaRule(), new BacklogRule(), new BranchRule()));
    }

    /** Creates a checker with your own rules (polymorphism: any {@link EligibilityRule} works). */
    public EligibilityChecker(List<EligibilityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** Checks one student against one company. It does not stop at the first failed rule. */
    public EligibilityResult check(Student student, Company company) {
        List<String> failedReasons = new ArrayList<>();
        for (EligibilityRule rule : rules) {
            EligibilityRule.Result result = rule.check(student, company);
            if (!result.passed()) {
                failedReasons.add(result.reason());
            }
        }
        return new EligibilityResult(student, company, failedReasons.isEmpty(), failedReasons);
    }

    /** Short yes/no form of {@link #check}. */
    public boolean isEligible(Student student, Company company) {
        return check(student, company).eligible();
    }

    /** One student against every company (one result per company, in the same order). */
    public List<EligibilityResult> checkAgainstAll(Student student, Collection<Company> companies) {
        List<EligibilityResult> results = new ArrayList<>();
        for (Company company : companies) {
            results.add(check(student, company));
        }
        return results;
    }

    /** Every student against one company (one result per student, in the same order). */
    public List<EligibilityResult> checkAllStudents(Company company, Collection<Student> students) {
        List<EligibilityResult> results = new ArrayList<>();
        for (Student student : students) {
            results.add(check(student, company));
        }
        return results;
    }

    /** Keeps only the students who are eligible for the company (the input order is kept). */
    public List<Student> eligibleStudents(Company company, Collection<Student> students) {
        List<Student> eligible = new ArrayList<>();
        for (Student student : students) {
            if (isEligible(student, company)) {
                eligible.add(student);
            }
        }
        return eligible;
    }
}
