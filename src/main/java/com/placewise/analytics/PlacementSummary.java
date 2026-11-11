package com.placewise.analytics;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Overall numbers for the placement report.
 *
 * @param totalStudents          students in the system
 * @param totalCompanies         companies in the system
 * @param totalSeats             sum of all company seats
 * @param eligibleForAny         students eligible for at least one company
 * @param eligibleForNone        students eligible for no company
 * @param avgEligibleCompanies   mean number of companies a student is eligible for, scale 2
 * @param applications           applications recorded
 * @param selected               applications with status SELECTED
 * @param waitlisted             applications with status WAITLISTED
 * @param rejected               applications with status REJECTED
 * @param withdrawn              applications with status WITHDRAWN
 * @param placedStudents         distinct students holding at least one SELECTED application
 * @param placementPercent       placed students as a percentage of all students, scale 2
 */
public record PlacementSummary(int totalStudents, int totalCompanies, int totalSeats, int eligibleForAny,
                               int eligibleForNone, BigDecimal avgEligibleCompanies, int applications,
                               int selected, int waitlisted, int rejected, int withdrawn,
                               int placedStudents, BigDecimal placementPercent) {

    /**
     * Flattens the summary into label/value pairs, in display order.
     * A {@link LinkedHashMap} keeps the insertion order that a plain {@code HashMap} would scramble.
     *
     * @return labelled values ready for a two-column report
     */
    public Map<String, String> toRows() {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Total students", String.valueOf(totalStudents));
        rows.put("Total companies", String.valueOf(totalCompanies));
        rows.put("Total seats", String.valueOf(totalSeats));
        rows.put("Students eligible for at least one company", String.valueOf(eligibleForAny));
        rows.put("Students eligible for no company", String.valueOf(eligibleForNone));
        rows.put("Average companies per student", avgEligibleCompanies.toPlainString());
        rows.put("Applications recorded", String.valueOf(applications));
        rows.put("Selected", String.valueOf(selected));
        rows.put("Waitlisted", String.valueOf(waitlisted));
        rows.put("Rejected", String.valueOf(rejected));
        rows.put("Withdrawn", String.valueOf(withdrawn));
        rows.put("Students placed", String.valueOf(placedStudents));
        rows.put("Placement percentage", placementPercent.toPlainString() + "%");
        return rows;
    }
}
