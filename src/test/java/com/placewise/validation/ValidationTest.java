package com.placewise.validation;

import static com.placewise.Fixtures.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.InvalidCompanyException;
import com.placewise.exception.InvalidStudentException;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationTest {

    private static Student s(String roll, String name, String branch, int sem, String cgpa, int backlogs,
                             String email) {
        return new Student(roll, name, branch, sem, new BigDecimal(cgpa), backlogs, email);
    }

    private static Student ok() {
        return s("R1", "Asha", "CSE", 4, "8.00", 0, null);
    }

    @ParameterizedTest
    @CsvSource({"0.00,true", "0,true", "5.55,true", "10.00,true", "10.004,true", "10.005,false",
            "10.01,false", "-0.01,false", "-1,false", "11,false"})
    void cgpaBoundaries(String cgpa, boolean valid) {
        Student student = s("R1", "Asha", "CSE", 4, cgpa, 0, null);
        assertThat(StudentValidator.problems(student).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "4,true", "8,true", "9,false", "-3,false"})
    void semesterBoundaries(int semester, boolean valid) {
        assertThat(StudentValidator.problems(s("R1", "Asha", "CSE", semester, "8", 0, null)).isEmpty())
                .isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"0,true", "1,true", "25,true", "-1,false", "-100,false"})
    void backlogBoundaries(int backlogs, boolean valid) {
        assertThat(StudentValidator.problems(s("R1", "Asha", "CSE", 4, "8", backlogs, null)).isEmpty())
                .isEqualTo(valid);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankRollNameAndBranchAreRejected(String blank) {
        assertThat(StudentValidator.problems(s(blank, "Asha", "CSE", 4, "8", 0, null)))
                .containsExactly("Roll number must not be blank");
        assertThat(StudentValidator.problems(s("R1", blank, "CSE", 4, "8", 0, null)))
                .containsExactly("Name must not be blank");
        assertThat(StudentValidator.problems(s("R1", "Asha", blank, 4, "8", 0, null)))
                .containsExactly("Branch must not be blank");
    }

    @ParameterizedTest
    @CsvSource({"a@b.com,true", "first.last@uni.edu.in,true", "no-at-sign,false", "a@b,false",
            "a b@c.com,false", "@c.com,false"})
    void emailFormat(String email, boolean valid) {
        assertThat(StudentValidator.problems(s("R1", "Asha", "CSE", 4, "8", 0, email)).isEmpty())
                .isEqualTo(valid);
    }

    @Test
    void validStudentHasNoProblemsAndEmailIsOptional() {
        assertThat(StudentValidator.problems(ok())).isEmpty();
        assertThat(StudentValidator.requireValid(ok())).isEqualTo(ok());
    }

    @Test
    void everyProblemIsReportedNotJustTheFirst() {
        Student bad = s("", "", "CSE", 9, "11", -1, "nope");
        assertThat(StudentValidator.problems(bad)).hasSize(6);
        assertThatThrownBy(() -> StudentValidator.requireValid(bad))
                .isInstanceOf(InvalidStudentException.class)
                .satisfies(e -> assertThat(((InvalidStudentException) e).problems()).hasSize(6))
                .hasMessageContaining("Roll number must not be blank")
                .hasMessageContaining("CGPA must be between 0.00 and 10.00");
    }

    @ParameterizedTest
    @CsvSource({"0.00,true", "10.00,true", "7.50,true", "10.01,false", "-0.01,false"})
    void companyMinCgpaBoundaries(String min, boolean valid) {
        Company c = new Company(0, "Acme", new BigDecimal(min), 0, Set.of(), BigDecimal.TEN, 1);
        assertThat(CompanyValidator.problems(c).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"1,true", "100,true", "0,false", "-5,false"})
    void companySeatsMustBePositive(int seats, boolean valid) {
        Company c = new Company(0, "Acme", new BigDecimal("7"), 0, Set.of(), BigDecimal.TEN, seats);
        assertThat(CompanyValidator.problems(c).isEmpty()).isEqualTo(valid);
    }

    @Test
    void companyOtherRules() {
        assertThat(CompanyValidator.problems(new Company(0, "  ", new BigDecimal("7"), 0, Set.of(),
                BigDecimal.TEN, 1))).containsExactly("Company name must not be blank");
        assertThat(CompanyValidator.problems(new Company(0, "A", new BigDecimal("7"), -1, Set.of(),
                BigDecimal.TEN, 1))).containsExactly("Maximum backlogs must not be negative");
        assertThat(CompanyValidator.problems(new Company(0, "A", new BigDecimal("7"), 0, Set.of(),
                new BigDecimal("-1"), 1))).containsExactly("Package must not be negative");
    }

    @Test
    void companyReportsAllProblemsAtOnce() {
        Company bad = new Company(0, "", new BigDecimal("12"), -1, Set.of(), new BigDecimal("-2"), 0);
        assertThatThrownBy(() -> CompanyValidator.requireValid(bad))
                .isInstanceOf(InvalidCompanyException.class)
                .satisfies(e -> assertThat(((InvalidCompanyException) e).problems()).hasSize(5));
        assertThat(CompanyValidator.requireValid(company("Acme", "7", 1, 2, "CSE")).name()).isEqualTo("Acme");
    }

    // ---- the generic Range.isBetween helper ---------------------------------------------------------

    @Test
    void isBetweenIncludesBothEndsForIntegers() {
        assertThat(Range.isBetween(2, 2, 4)).isTrue();
        assertThat(Range.isBetween(4, 2, 4)).isTrue();
        assertThat(Range.isBetween(1, 2, 4)).isFalse();
        assertThat(Range.isBetween(5, 2, 4)).isFalse();
    }

    @Test
    void isBetweenWorksForAnyComparableTypeSuchAsBigDecimal() {
        assertThat(Range.isBetween(new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.TEN)).isTrue();
        assertThat(Range.isBetween(new BigDecimal("10.01"), BigDecimal.ZERO, BigDecimal.TEN)).isFalse();
        assertThat(Range.isBetween("b", "a", "c")).isTrue();
    }

    @Test
    void allProblemsAreReportedInRuleOrder() {
        Student bad = s(" ", " ", " ", 9, "11", -1, "not-an-email");
        assertThat(StudentValidator.problems(bad)).containsExactly(
                "Roll number must not be blank", "Name must not be blank", "Branch must not be blank",
                "Semester must be between 1 and 8", "CGPA must be between 0.00 and 10.00",
                "Active backlogs must not be negative", "Email address is not valid");
        assertThat(StudentValidator.problems(ok())).isEmpty();
    }
}
