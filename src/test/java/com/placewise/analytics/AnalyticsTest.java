package com.placewise.analytics;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.eligibility.EligibilityRule;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AnalyticsTest {

    private final EligibilityChecker checker = new EligibilityChecker();

    // ---- branch summary / averages ----------------------------------------------------------------

    @Test
    void branchSummaryCountsAverageAndHighestPerBranch() {
        List<Student> students = List.of(
                student("R1", "Ann", "CSE", "8.00", 0), student("R2", "Bob", "CSE", "9.00", 0),
                student("R3", "Cy", "CSE", "7.51", 0), student("R4", "Di", "IT", "6.00", 0));
        List<BranchSummary> summaries = StudentAnalytics.branchSummaries(students);
        assertThat(summaries).extracting(BranchSummary::branch).containsExactly("CSE", "IT");
        BranchSummary cse = summaries.get(0);
        assertThat(cse.count()).isEqualTo(3);
        assertThat(cse.averageCgpa()).isEqualTo(new BigDecimal("8.17")); // 24.51 / 3 = 8.17
        assertThat(cse.highestCgpa()).isEqualTo(new BigDecimal("9.00"));
        assertThat(summaries.get(1).averageCgpa()).isEqualTo(new BigDecimal("6.00"));
    }

    @Test
    void emptyInputGivesNoSummariesAndZeroAverage() {
        assertThat(StudentAnalytics.branchSummaries(List.of())).isEmpty();
        assertThat(StudentAnalytics.averageCgpa(List.of())).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void averageRoundsHalfUp() {
        // (7.00 + 7.01) / 2 = 7.005 -> 7.01
        assertThat(StudentAnalytics.averageCgpa(List.of(student("A", "7.00", 0), student("B", "7.01", 0))))
                .isEqualTo(new BigDecimal("7.01"));
    }

    // ---- placement summary ------------------------------------------------------------------------

    @Test
    void placementSummaryIsHandComputed() {
        Company a = company(1, "A", "8.00", 0, 2);
        Company b = company(2, "B", "9.50", 0, 3, "IT");
        Student s1 = student("R1", "9.00", 0); // eligible A only
        Student s2 = student("R2", "7.00", 0); // eligible none
        Student s3 = student("R3", "Zo", "IT", "9.80", 0); // eligible A and B
        Student s4 = student("R4", "8.50", 0); // eligible A only
        List<Application> apps = List.of(
                new Application(s1, a, ApplicationStatus.SELECTED),
                new Application(s3, a, ApplicationStatus.SELECTED),
                new Application(s3, b, ApplicationStatus.SELECTED),
                new Application(s4, a, ApplicationStatus.WAITLISTED),
                new Application(s2, a, ApplicationStatus.REJECTED),
                new Application(s2, b, ApplicationStatus.WITHDRAWN));
        PlacementSummary p = StudentAnalytics.placementSummary(List.of(s1, s2, s3, s4), List.of(a, b), apps, checker);
        assertThat(p.totalStudents()).isEqualTo(4);
        assertThat(p.totalCompanies()).isEqualTo(2);
        assertThat(p.totalSeats()).isEqualTo(5);
        assertThat(p.eligibleForAny()).isEqualTo(3);
        assertThat(p.eligibleForNone()).isEqualTo(1);
        assertThat(p.avgEligibleCompanies()).isEqualTo(new BigDecimal("1.00")); // 4 pairs / 4 students
        assertThat(p.applications()).isEqualTo(6);
        assertThat(p.selected()).isEqualTo(3);
        assertThat(p.waitlisted()).isEqualTo(1);
        assertThat(p.rejected()).isEqualTo(1);
        assertThat(p.withdrawn()).isEqualTo(1);
        assertThat(p.placedStudents()).isEqualTo(2); // s3 holds two offers but counts once
        assertThat(p.placementPercent()).isEqualTo(new BigDecimal("50.00"));
        Map<String, String> rows = p.toRows();
        assertThat(rows).containsEntry("Total students", "4").containsEntry("Placement percentage", "50.00%");
        assertThat(rows.keySet().iterator().next()).isEqualTo("Total students"); // insertion order kept
    }

    @Test
    void placementSummaryOfNothingIsAllZero() {
        PlacementSummary p = StudentAnalytics.placementSummary(List.of(), List.of(), List.of(), checker);
        assertThat(p.totalStudents()).isZero();
        assertThat(p.avgEligibleCompanies()).isEqualTo(new BigDecimal("0.00"));
        assertThat(p.placementPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    // ---- explainer --------------------------------------------------------------------------------

    private final WhyNotEligibleExplainer explainer = new WhyNotEligibleExplainer(checker);

    @Test
    void explanationMatchesTheSpecExample() {
        Explanation e = explainer.explain(student("R1", "6.80", 2), company("Acme", "7.50", 1, 1));
        assertThat(e.eligible()).isFalse();
        assertThat(e.cgpaShortfall()).isEqualTo(new BigDecimal("0.70"));
        assertThat(e.backlogExcess()).isEqualTo(1);
        assertThat(e.summary()).isEqualTo("Needs +0.70 CGPA and 1 fewer backlog");
        assertThat(e.needs()).containsExactly("+0.70 CGPA", "1 fewer backlog");
    }

    @Test
    void pluralBacklogsAndBranchGap() {
        Explanation e = explainer.explain(student("R1", "Ann", "ECE", "9.00", 4), company("Acme", "7", 1, 1, "CSE", "IT"));
        assertThat(e.summary()).isEqualTo(
                "Needs 3 fewer backlogs; Branch ECE is not allowed (allowed: CSE, IT)");
        assertThat(e.branchBlocked()).isTrue();
    }

    @Test
    void onlyOneGapOnlyOneNeed() {
        assertThat(explainer.explain(student("R1", "6.00", 0), company("A", "7", 0, 1)).summary())
                .isEqualTo("Needs +1.00 CGPA");
        assertThat(explainer.explain(student("R1", "9.00", 2), company("A", "7", 0, 1)).summary())
                .isEqualTo("Needs 2 fewer backlogs");
    }

    @Test
    void exactBoundaryHasNoGapAndIsEligible() {
        Explanation e = explainer.explain(student("R1", "7.50", 1), company("A", "7.50", 1, 1));
        assertThat(e.eligible()).isTrue();
        assertThat(e.cgpaShortfall()).isEqualTo(new BigDecimal("0.00"));
        assertThat(e.backlogExcess()).isZero();
        assertThat(e.summary()).isEqualTo("Eligible: meets every criterion");
    }

    @Test
    void customRuleFailureWithoutNumericGapStillExplains() {
        EligibilityChecker strict = new EligibilityChecker(List.of((s, c) ->
                com.placewise.eligibility.EligibilityRule.Result.fail("closed")));
        Explanation e = new WhyNotEligibleExplainer(strict).explain(student("R1", "9", 0), company("A", "7", 0, 1));
        assertThat(e.summary()).isEqualTo("Not eligible under the configured rules");
    }

    @Test
    void explanationAgreesWithCheckerOnRandomPairs() {
        Random random = new Random(11);
        for (int i = 0; i < 2_000; i++) {
            Student s = new Student("R" + i, "N", i % 2 == 0 ? "CSE" : "IT", 4,
                    BigDecimal.valueOf(random.nextInt(1001), 2), random.nextInt(4), null);
            Company c = company("C", String.valueOf(5 + random.nextInt(5)), random.nextInt(3), 1,
                    i % 3 == 0 ? new String[] {"CSE"} : new String[0]);
            Explanation e = explainer.explain(s, c);
            boolean noGap = e.cgpaShortfall().signum() == 0 && e.backlogExcess() == 0 && !e.branchBlocked();
            assertThat(e.eligible()).isEqualTo(noGap).isEqualTo(checker.isEligible(s, c));
        }
    }

    @Test
    void explainRejectionsListsOnlyTheCompaniesThatSaidNo() {
        Student s = student("R1", "7.60", 0);
        List<Company> companies = List.of(company("Low", "7", 0, 1), company("High", "9", 0, 1),
                company("Mid", "8", 0, 1));
        assertThat(explainer.explainRejections(s, companies)).extracting(x -> x.company().name())
                .containsExactly("High", "Mid");
    }

    // ---- what-if ----------------------------------------------------------------------------------

    private final WhatIfSimulator whatIf = new WhatIfSimulator(checker);

    private final List<Company> companies = List.of(
            company(1, "A", "7.50", 0, 1),        // needs 7.50 and no backlogs
            company(2, "B", "8.00", 1, 1),        // needs 8.00, one backlog allowed
            company(3, "C", "9.00", 0, 1),        // far away
            company(4, "D", "6.00", 2, 1, "IT")); // wrong branch for the CSE student

    @Test
    void whatIfUnlocksAreHandComputed() {
        Student ann = student("R1", "Ann", "CSE", "7.00", 2);
        // +0.60 CGPA -> 7.60 ; clear 2 backlogs -> 0 : A unlocks (7.60>=7.50, 0<=0); B still 7.60<8.00
        WhatIfResult r = whatIf.simulate(ann, companies, new BigDecimal("0.60"), 2);
        assertThat(r.scenario().cgpa()).isEqualTo(new BigDecimal("7.60"));
        assertThat(r.scenario().activeBacklogs()).isZero();
        assertThat(r.alreadyEligible()).isEmpty();
        assertThat(r.newlyUnlocked()).extracting(Company::name).containsExactly("A");
        assertThat(r.stillBlocked()).extracting(Company::name).containsExactly("B", "C", "D");
    }

    @Test
    void whatIfOnlyBacklogsClearedUnlocksBacklogGatedCompanies() {
        Student ann = student("R1", "Ann", "CSE", "8.20", 3);
        WhatIfResult none = whatIf.simulate(ann, companies, BigDecimal.ZERO, 1); // 2 left: B needs <=1
        assertThat(none.newlyUnlocked()).isEmpty();
        WhatIfResult b = whatIf.simulate(ann, companies, BigDecimal.ZERO, 2);    // 1 left: B unlocks
        assertThat(b.newlyUnlocked()).extracting(Company::name).containsExactly("B");
    }

    @Test
    void alreadyEligibleCompaniesAreNotCountedAsUnlocked() {
        Student ann = student("R1", "Ann", "CSE", "8.50", 0);
        WhatIfResult r = whatIf.simulate(ann, companies, new BigDecimal("0.60"), 0);
        assertThat(r.alreadyEligible()).extracting(Company::name).containsExactly("A", "B");
        assertThat(r.newlyUnlocked()).extracting(Company::name).containsExactly("C"); // 9.10 >= 9.00
    }

    @Test
    void whatIfCapsCgpaAtTenAndBacklogsAtZero() {
        Student s = student("R1", "9.80", 1);
        Student scenario = whatIf.apply(s, new BigDecimal("5"), 9);
        assertThat(scenario.cgpa()).isEqualTo(new BigDecimal("10.00"));
        assertThat(scenario.activeBacklogs()).isZero();
    }

    @Test
    void whatIfNeverModifiesRealData() {
        Student ann = student("R1", "Ann", "CSE", "7.00", 2);
        Student before = ann.withActiveBacklogs(2); // an equal copy to compare against
        List<Company> companySnapshot = new ArrayList<>(companies);
        whatIf.simulate(ann, companies, new BigDecimal("2"), 2);
        whatIf.cohortUnlocks(List.of(ann), companies, new BigDecimal("2"), 2);
        assertThat(ann).isEqualTo(before);
        assertThat(ann.cgpa()).isEqualTo(new BigDecimal("7.00"));
        assertThat(ann.activeBacklogs()).isEqualTo(2);
        assertThat(companies).isEqualTo(companySnapshot);
    }

    @Test
    void negativeImprovementsAreRejected() {
        Student s = student("R1", "7", 1);
        assertThatThrownBy(() -> whatIf.apply(s, new BigDecimal("-0.1"), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> whatIf.apply(s, BigDecimal.ZERO, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cohortUnlocksCountsNewlyEligibleStudentsPerCompany() {
        List<Student> students = List.of(
                student("R1", "Ann", "CSE", "7.00", 0),  // +0.5 -> 7.50: unlocks A
                student("R2", "Bob", "CSE", "7.00", 0),  // same
                student("R3", "Cy", "CSE", "7.60", 0),   // already eligible for A: no unlock for A
                student("R4", "Di", "IT", "5.60", 3));   // +0.5 -> 6.10, 3 backlogs: D needs <=2 -> blocked
        Map<String, Long> unlocks = whatIf.cohortUnlocks(students, companies, new BigDecimal("0.50"), 0);
        assertThat(unlocks).containsEntry("A", 2L).containsEntry("B", 1L) // Cy: 7.60 -> 8.10 unlocks B
                .containsEntry("C", 0L).containsEntry("D", 0L);
        assertThat(unlocks.keySet()).containsExactly("A", "B", "C", "D"); // alphabetical
    }
}
