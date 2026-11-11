package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;

/** Passes when the student's CGPA is <em>at least</em> the company's minimum (equal is fine). */
public final class CgpaRule implements EligibilityRule {

    /** Creates the rule (it has no settings: the minimum comes from each company). */
    public CgpaRule() {
        // stateless
    }

    @Override
    public Result check(Student student, Company company) {
        // compareTo, never equals: 7.50 and 7.5 are equal numbers but unequal BigDecimals.
        if (student.cgpa().compareTo(company.minCgpa()) >= 0) {
            return Result.pass("CGPA " + student.cgpa() + " meets required " + company.minCgpa());
        }
        return Result.fail("CGPA " + student.cgpa() + " is below required " + company.minCgpa());
    }
}
