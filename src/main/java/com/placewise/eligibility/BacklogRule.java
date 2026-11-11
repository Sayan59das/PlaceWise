package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;

/** Passes when the student's active backlogs are <em>at most</em> the company's maximum. */
public final class BacklogRule implements EligibilityRule {

    /** Creates the rule (it has no settings: the maximum comes from each company). */
    public BacklogRule() {
        // stateless
    }

    @Override
    public Result check(Student student, Company company) {
        int backlogs = student.activeBacklogs();
        if (backlogs <= company.maxBacklogs()) {
            return Result.pass(backlogs + " active backlog(s) within allowed " + company.maxBacklogs());
        }
        return Result.fail(backlogs + " active backlog(s) exceed the allowed maximum of "
                + company.maxBacklogs());
    }
}
