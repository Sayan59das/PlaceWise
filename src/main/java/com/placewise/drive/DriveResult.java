package com.placewise.drive;

import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Everything a finished drive produced.
 *
 * @param config       the settings used
 * @param companies    the companies, in board order
 * @param applications every application ever submitted, with its final status
 * @param boards       final allocation per company (same order as {@code companies})
 * @param elapsed      wall-clock time of the run
 */
public record DriveResult(DriveConfig config, List<Company> companies, List<Application> applications,
                          List<BoardSnapshot> boards, Duration elapsed) {

    /**
     * Counts applications in a status.
     *
     * @param status the status
     * @return how many applications have it
     */
    public long count(ApplicationStatus status) {
        long count = 0;
        for (Application application : applications) {
            if (application.status() == status) {
                count++;
            }
        }
        return count;
    }

    /**
     * Distinct students who hold at least one seat.
     *
     * @return the number of placed students
     */
    public long placedStudents() {
        Set<String> placed = new HashSet<>();  // a Set ignores repeats: two offers = one placed student
        for (Application application : applications) {
            if (application.status() == ApplicationStatus.SELECTED) {
                placed.add(application.student().rollNo());
            }
        }
        return placed.size();
    }

    /**
     * Console summary.
     *
     * @return one line per company plus totals
     */
    public List<String> summaryLines() {
        List<String> lines = new ArrayList<>();
        for (BoardSnapshot board : boards) {
            Company company = board.company();
            lines.add(String.format("%-20s seats %3d | selected %3d | waitlisted %3d", company.name(),
                    company.seats(), board.selected().size(), board.waitlisted().size()));
        }
        lines.add("Applications: " + applications.size() + " (selected " + count(ApplicationStatus.SELECTED)
                + ", waitlisted " + count(ApplicationStatus.WAITLISTED) + ", rejected "
                + count(ApplicationStatus.REJECTED) + ", withdrawn " + count(ApplicationStatus.WITHDRAWN) + ")");
        lines.add("Students placed: " + placedStudents() + " | time: " + elapsed.toMillis() + " ms");
        return lines;
    }
}
