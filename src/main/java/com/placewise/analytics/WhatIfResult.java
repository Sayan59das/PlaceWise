package com.placewise.analytics;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.util.List;

/**
 * Outcome of one what-if scenario for one student.
 *
 * @param original        the real student (unchanged)
 * @param scenario        the hypothetical student with the improvements applied
 * @param alreadyEligible companies the real student is eligible for today
 * @param newlyUnlocked   companies that become eligible only in the scenario
 * @param stillBlocked    companies that stay out of reach (for example because of branch)
 */
public record WhatIfResult(Student original, Student scenario, List<Company> alreadyEligible,
                           List<Company> newlyUnlocked, List<Company> stillBlocked) {
}
