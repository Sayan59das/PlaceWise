package com.placewise.analytics;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Explains eligibility verdicts with exact gaps. The verdict itself always comes from the checker. */
public final class WhyNotEligibleExplainer {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final EligibilityChecker checker;

    /**
     * Creates the explainer.
     *
     * @param checker the checker whose verdicts are explained
     */
    public WhyNotEligibleExplainer(EligibilityChecker checker) {
        this.checker = checker;
    }

    /**
     * Explains one student/company pair.
     *
     * @param student the applicant
     * @param company the company
     * @return the verdict plus the gap on every criterion
     */
    public Explanation explain(Student student, Company company) {
        BigDecimal shortfall = company.minCgpa().subtract(student.cgpa()).max(ZERO);
        int excess = Math.max(0, student.activeBacklogs() - company.maxBacklogs());
        boolean branchBlocked = !company.allowsBranch(student.branch());
        return new Explanation(student, company, checker.isEligible(student, company),
                shortfall, excess, branchBlocked);
    }

    /**
     * Explains every company the student is <em>not</em> eligible for.
     *
     * @param student   the applicant
     * @param companies the companies to test
     * @return one explanation per rejecting company, in input order
     */
    public List<Explanation> explainRejections(Student student, Collection<Company> companies) {
        return companies.stream()
                .map(company -> explain(student, company))
                .filter(explanation -> !explanation.eligible())
                .toList();
    }
}
