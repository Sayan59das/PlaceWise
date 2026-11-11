package com.placewise.analytics;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The answer to "why is this student not eligible?": the exact gap on every failed rule.
 *
 * @param student        the applicant
 * @param company        the company
 * @param eligible       whether the student is eligible (then every gap is zero/false)
 * @param cgpaShortfall  how much CGPA is missing, {@code 0.00} if none
 * @param backlogExcess  how many backlogs must be cleared, 0 if none
 * @param branchBlocked  {@code true} if the student's branch is not accepted
 */
public record Explanation(Student student, Company company, boolean eligible, BigDecimal cgpaShortfall,
                          int backlogExcess, boolean branchBlocked) {

    /**
     * Lists the numeric improvements needed.
     *
     * @return for example {@code ["+0.70 CGPA", "1 fewer backlog"]}; empty when none are needed
     */
    public List<String> needs() {
        List<String> needs = new ArrayList<>();
        if (cgpaShortfall.signum() > 0) {
            needs.add("+" + cgpaShortfall.toPlainString() + " CGPA");
        }
        if (backlogExcess > 0) {
            needs.add(backlogExcess + (backlogExcess == 1 ? " fewer backlog" : " fewer backlogs"));
        }
        return needs;
    }

    /**
     * One sentence covering every failed rule.
     *
     * @return for example {@code "Needs +0.70 CGPA and 1 fewer backlog"}
     */
    public String summary() {
        if (eligible) {
            return "Eligible: meets every criterion";
        }
        List<String> parts = new ArrayList<>();
        List<String> needs = needs();
        if (!needs.isEmpty()) {
            parts.add("Needs " + String.join(" and ", needs));
        }
        if (branchBlocked) {
            parts.add("Branch " + student.branch() + " is not allowed (allowed: "
                    + company.branchesLabel() + ")");
        }
        if (parts.isEmpty()) {
            parts.add("Not eligible under the configured rules");
        }
        return String.join("; ", parts);
    }
}
