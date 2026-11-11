package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.util.List;

/**
 * The verdict for one student and one company.
 *
 * @param student       the applicant
 * @param company       the company
 * @param eligible      {@code true} when every rule passed
 * @param failedReasons the reason of <em>every</em> failed rule (empty when eligible)
 */
public record EligibilityResult(Student student, Company company, boolean eligible,
                                List<String> failedReasons) {

    /** Makes the reason list read-only so the record is truly immutable. */
    public EligibilityResult {
        failedReasons = List.copyOf(failedReasons);
    }

    /**
     * One line for the console.
     *
     * @return for example {@code "ELIGIBLE"} or {@code "NOT ELIGIBLE - CGPA 6.80 is below required 7.50"}
     */
    public String describe() {
        return eligible ? "ELIGIBLE" : "NOT ELIGIBLE - " + String.join("; ", failedReasons);
    }
}
