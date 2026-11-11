package com.placewise.analytics;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Statistics over students and applications. Nothing here changes data or touches files. */
public final class StudentAnalytics {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private StudentAnalytics() {
    }

    /** One summary per branch, in alphabetical branch order. */
    public static List<BranchSummary> branchSummaries(Collection<Student> students) {
        // Step 1: put the students into groups, one group per branch. A TreeMap keeps the keys sorted.
        Map<String, List<Student>> byBranch = new TreeMap<>();
        for (Student student : students) {
            List<Student> group = byBranch.get(student.branch());
            if (group == null) {
                group = new ArrayList<>();
                byBranch.put(student.branch(), group);
            }
            group.add(student);
        }
        // Step 2: summarize each group.
        List<BranchSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Student>> entry : byBranch.entrySet()) {
            List<Student> group = entry.getValue();
            summaries.add(new BranchSummary(entry.getKey(), group.size(), averageCgpa(group), highestCgpa(group)));
        }
        return summaries;
    }

    private static BigDecimal highestCgpa(List<Student> group) {
        BigDecimal highest = ZERO;
        for (Student student : group) {
            if (student.cgpa().compareTo(highest) > 0) {
                highest = student.cgpa();
            }
        }
        return highest;
    }

    /** The mean CGPA rounded to 2 decimals; {@code 0.00} when there are no students. */
    public static BigDecimal averageCgpa(Collection<Student> students) {
        if (students.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Student student : students) {
            sum = sum.add(student.cgpa());
        }
        return sum.divide(BigDecimal.valueOf(students.size()), 2, RoundingMode.HALF_UP);
    }

    /** Builds the overall placement summary (applications may be empty if no drive has run yet). */
    public static PlacementSummary placementSummary(Collection<Student> students,
                                                    Collection<Company> companies,
                                                    Collection<Application> applications,
                                                    EligibilityChecker checker) {
        long eligiblePairs = 0;   // how many (student, company) pairs are eligible
        int eligibleForAny = 0;   // how many students are eligible for at least one company
        for (Student student : students) {
            int count = 0;
            for (Company company : companies) {
                if (checker.isEligible(student, company)) {
                    count++;
                }
            }
            eligiblePairs += count;
            if (count > 0) {
                eligibleForAny++;
            }
        }
        int totalSeats = 0;
        for (Company company : companies) {
            totalSeats += company.seats();
        }
        // A student selected by two companies is still one placed student, so count roll numbers in a Set.
        Set<String> placedStudents = new HashSet<>();
        for (Application application : applications) {
            if (application.status() == ApplicationStatus.SELECTED) {
                placedStudents.add(application.student().rollNo());
            }
        }
        int placed = placedStudents.size();
        BigDecimal averageOffers = students.isEmpty() ? ZERO
                : BigDecimal.valueOf(eligiblePairs).divide(BigDecimal.valueOf(students.size()), 2, RoundingMode.HALF_UP);
        BigDecimal placedPercent = students.isEmpty() ? ZERO
                : BigDecimal.valueOf(placed).multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(students.size()), 2, RoundingMode.HALF_UP);
        return new PlacementSummary(
                students.size(), companies.size(), totalSeats, eligibleForAny,
                students.size() - eligibleForAny, averageOffers, applications.size(),
                countStatus(applications, ApplicationStatus.SELECTED),
                countStatus(applications, ApplicationStatus.WAITLISTED),
                countStatus(applications, ApplicationStatus.REJECTED),
                countStatus(applications, ApplicationStatus.WITHDRAWN),
                placed, placedPercent);
    }

    private static int countStatus(Collection<Application> applications, ApplicationStatus status) {
        int count = 0;
        for (Application application : applications) {
            if (application.status() == status) {
                count++;
            }
        }
        return count;
    }
}
