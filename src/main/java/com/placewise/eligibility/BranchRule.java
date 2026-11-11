package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;

/** Passes when the company accepts the student's branch; an empty branch list accepts every branch. */
public final class BranchRule implements EligibilityRule {

    /** Creates the rule (it has no settings: the allowed branches come from each company). */
    public BranchRule() {
        // stateless
    }

    @Override
    public Result check(Student student, Company company) {
        if (company.allowsBranch(student.branch())) {
            return Result.pass("Branch " + student.branch() + " is accepted");
        }
        return Result.fail("Branch " + student.branch() + " is not allowed (allowed: "
                + company.branchesLabel() + ")");
    }
}
