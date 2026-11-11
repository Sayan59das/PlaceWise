package com.placewise.eligibility;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EligibilityTest {

    private final EligibilityChecker checker = new EligibilityChecker();

    @ParameterizedTest
    @CsvSource({"7.50,7.50,true", "7.51,7.50,true", "7.49,7.50,false", "7.50,7.5,true", "0.00,0,true",
            "10.00,10,true", "9.99,10,false"})
    void cgpaRuleBoundary(String cgpa, String min, boolean pass) {
        Student s = student("R1", cgpa, 0);
        Company c = company("Acme", min, 5, 1);
        assertThat(new CgpaRule().check(s, c).passed()).isEqualTo(pass);
    }

    @Test
    void cgpaRuleReasonMatchesTheSpecExample() {
        EligibilityRule.Result r = new CgpaRule().check(student("R1", "6.80", 0), company("Acme", "7.50", 5, 1));
        assertThat(r.passed()).isFalse();
        assertThat(r.reason()).isEqualTo("CGPA 6.80 is below required 7.50");
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "1,1,true", "2,1,false", "0,5,true", "5,5,true", "6,5,false"})
    void backlogRuleBoundary(int backlogs, int max, boolean pass) {
        Student s = student("R1", "8.00", backlogs);
        Company c = company("Acme", "6", max, 1);
        assertThat(new BacklogRule().check(s, c).passed()).isEqualTo(pass);
    }

    @Test
    void branchRuleAllowsListedBranchOnly() {
        Company c = company("Acme", "6", 0, 1, "CSE", "IT");
        assertThat(new BranchRule().check(student("R1", "Ann", "cse", "8", 0), c).passed()).isTrue();
        EligibilityRule.Result denied = new BranchRule().check(student("R2", "Bob", "ECE", "8", 0), c);
        assertThat(denied.passed()).isFalse();
        assertThat(denied.reason()).isEqualTo("Branch ECE is not allowed (allowed: CSE, IT)");
    }

    @Test
    void emptyBranchListMeansAllBranches() {
        Company open = company("Open", "6", 0, 1);
        assertThat(checker.isEligible(student("R1", "Ann", "MECH", "8", 0), open)).isTrue();
        assertThat(checker.isEligible(student("R2", "Bob", "ARCH", "8", 0), open)).isTrue();
    }

    @Test
    void passesExactlyOnTheBoundaries() {
        Company c = company("Acme", "7.50", 2, 1, "CSE");
        assertThat(checker.isEligible(student("R1", "7.50", 2), c)).isTrue();
    }

    @Test
    void failsJustBelowTheBoundaries() {
        Company c = company("Acme", "7.50", 2, 1, "CSE");
        assertThat(checker.isEligible(student("R1", "7.49", 2), c)).isFalse();
        assertThat(checker.isEligible(student("R2", "7.50", 3), c)).isFalse();
    }

    @Test
    void everyFailureIsReportedNotJustTheFirst() {
        Company c = company("Acme", "7.50", 1, 1, "CSE");
        EligibilityResult r = checker.check(student("R1", "Zed", "ECE", "6.80", 3), c);
        assertThat(r.eligible()).isFalse();
        assertThat(r.failedReasons()).containsExactly(
                "CGPA 6.80 is below required 7.50",
                "3 active backlog(s) exceed the allowed maximum of 1",
                "Branch ECE is not allowed (allowed: CSE)");
        assertThat(r.describe()).startsWith("NOT ELIGIBLE - CGPA 6.80");
    }

    @Test
    void eligibleResultHasNoReasons() {
        EligibilityResult r = checker.check(student("R1", "8", 0), company("Acme", "7", 0, 1));
        assertThat(r.eligible()).isTrue();
        assertThat(r.failedReasons()).isEmpty();
        assertThat(r.describe()).isEqualTo("ELIGIBLE");
    }

    @Test
    void oneStudentAgainstAllCompanies() {
        Student s = student("R1", "Ann", "CSE", "7.60", 1);
        List<Company> companies = List.of(
                company(1, "A", "7.00", 1, 1),
                company(2, "B", "8.00", 1, 1),
                company(3, "C", "7.00", 0, 1),
                company(4, "D", "7.00", 1, 1, "IT"));
        List<EligibilityResult> results = checker.checkAgainstAll(s, companies);
        assertThat(results).extracting(EligibilityResult::eligible).containsExactly(true, false, false, false);
        assertThat(results).extracting(r -> r.company().name()).containsExactly("A", "B", "C", "D");
    }

    @Test
    void oneCompanyAgainstAllStudents() {
        Company c = company("Acme", "7.00", 0, 1);
        List<Student> students = List.of(student("R1", "7.00", 0), student("R2", "6.99", 0),
                student("R3", "9.00", 1), student("R4", "9.00", 0));
        assertThat(checker.checkAllStudents(c, students)).extracting(EligibilityResult::eligible)
                .containsExactly(true, false, false, true);
        assertThat(checker.eligibleStudents(c, students)).extracting(Student::rollNo)
                .containsExactly("R1", "R4");
    }

    @Test
    void checkerKeepsEveryFailureReasonInsteadOfStoppingAtTheFirst() {
        Company c = company("Acme", "7.50", 0, 1, "IT");
        EligibilityResult bad = checker.check(student("R1", "Ann", "CSE", "6.00", 2), c);
        assertThat(bad.eligible()).isFalse();
        assertThat(bad.failedReasons()).hasSize(3);
        assertThat(bad.describe()).contains("CGPA 6.00 is below").contains("backlog").contains("Branch CSE");
        assertThat(checker.check(student("R2", "Bob", "IT", "9.00", 0), c).eligible()).isTrue();
    }

    @Test
    void aRuleCanBeAPlainLambda() {
        EligibilityRule alwaysNo = (s, c) -> EligibilityRule.Result.fail("closed");
        EligibilityChecker custom = new EligibilityChecker(List.of(new CgpaRule(), alwaysNo));
        EligibilityResult r = custom.check(student("R1", "9", 0), company("Acme", "7", 0, 1));
        assertThat(r.eligible()).isFalse();
        assertThat(r.failedReasons()).containsExactly("closed");
    }

    @Test
    void resultReasonListIsReadOnly() {
        EligibilityResult r = checker.check(student("R1", "5", 0), company("Acme", "7", 0, 1));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> r.failedReasons().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
