package com.placewise.analytics;

import java.math.BigDecimal;

/**
 * Statistics for one branch.
 *
 * @param branch      branch code
 * @param count       number of students
 * @param averageCgpa mean CGPA, scale 2
 * @param highestCgpa best CGPA, scale 2
 */
public record BranchSummary(String branch, long count, BigDecimal averageCgpa, BigDecimal highestCgpa) {
}
