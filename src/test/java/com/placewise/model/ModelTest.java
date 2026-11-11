package com.placewise.model;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModelTest {

    @ParameterizedTest
    @CsvSource({"0,0.00", "10,10.00", "7.5,7.50", "7.555,7.56", "7.554,7.55", "9.999,10.00"})
    void normalizeGivesTwoDecimalsHalfUp(String raw, String expected) {
        assertThat(Cgpa.normalize(new BigDecimal(raw))).isEqualTo(new BigDecimal(expected));
        assertThat(Cgpa.normalize(new BigDecimal(raw)).scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"0.00,true", "10.00,true", "5.00,true", "10.01,false", "-0.01,false", "100.00,false"})
    void inRangeCoversBoundaries(String value, boolean expected) {
        assertThat(Cgpa.inRange(new BigDecimal(value))).isEqualTo(expected);
    }

    @Test
    void parseTrimsAndRejectsGarbage() {
        assertThat(Cgpa.parse(" 8.5 ")).isEqualTo(new BigDecimal("8.50"));
        assertThatThrownBy(() -> Cgpa.parse("abc")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void normalizeRejectsNull() {
        assertThatThrownBy(() -> Cgpa.normalize(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void studentConstructorNormalizesText() {
        Student s = new Student("  cs101 ", "  Asha Rao ", " cse ", 5, new BigDecimal("8.5"), 0, "  ");
        assertThat(s.rollNo()).isEqualTo("CS101");
        assertThat(s.name()).isEqualTo("Asha Rao");
        assertThat(s.branch()).isEqualTo("CSE");
        assertThat(s.cgpa()).isEqualTo(new BigDecimal("8.50"));
        assertThat(s.email()).isNull();
        assertThat(s.optionalEmail()).isEmpty();
    }

    @Test
    void studentNullTextBecomesEmpty() {
        Student s = new Student(null, null, null, 1, BigDecimal.ONE, 0, null);
        assertThat(s.rollNo()).isEmpty();
        assertThat(s.name()).isEmpty();
        assertThat(s.branch()).isEmpty();
    }

    @Test
    void studentKeepsEmail() {
        Student s = new Student("A1", "A", "CSE", 1, BigDecimal.ONE, 0, " a@b.com ");
        assertThat(s.optionalEmail()).contains("a@b.com");
    }

    @Test
    void withMethodsReturnCopiesAndLeaveOriginalAlone() {
        Student original = student("R1", "7.00", 2);
        Student better = original.withCgpa(new BigDecimal("8")).withActiveBacklogs(0);
        assertThat(better.cgpa()).isEqualTo(new BigDecimal("8.00"));
        assertThat(better.activeBacklogs()).isZero();
        assertThat(original.cgpa()).isEqualTo(new BigDecimal("7.00"));
        assertThat(original.activeBacklogs()).isEqualTo(2);
    }

    @Test
    void studentsCompareByRollNumberAndAreValueEqual() {
        assertThat(student("A1", "5.00", 0)).isLessThan(student("B1", "9.00", 0));
        // same person typed differently: roll case and CGPA scale are normalized away
        assertThat(student("A1", "Amy", "CSE", "5.00", 0)).isEqualTo(student("a1", "Amy", "cse", "5", 0));
        assertThat(student("A1", "Amy", "CSE", "5.00", 0)).isNotEqualTo(student("A1", "Amy", "CSE", "5.01", 0));
        assertThat(new HashSet<>(Set.of(student("A1", "5.00", 0)))).contains(student("A1", "5.0", 0));
    }

    @Test
    void companyNormalizesBranchesIntoSortedReadOnlySet() {
        Company c = company("Acme", "7", 1, 3, " it ", "cse", "", "CSE");
        assertThat(c.allowedBranches()).containsExactly("CSE", "IT");
        assertThat(c.branchesLabel()).isEqualTo("CSE, IT");
        assertThatThrownBy(() -> c.allowedBranches().add("ECE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyBranchSetAllowsEverything() {
        Company c = company("Open", "6", 0, 1);
        assertThat(c.allowedBranches()).isEmpty();
        assertThat(c.allowsBranch("ANYTHING")).isTrue();
        assertThat(c.branchesLabel()).isEqualTo("ALL");
    }

    @Test
    void allowsBranchIgnoresCase() {
        Company c = company("Acme", "6", 0, 1, "CSE");
        assertThat(c.allowsBranch(" cse ")).isTrue();
        assertThat(c.allowsBranch("ECE")).isFalse();
    }

    @Test
    void companyNullsAreNormalized() {
        Company c = new Company(0, null, new BigDecimal("7"), 0, null, null, 1);
        assertThat(c.name()).isEmpty();
        assertThat(c.allowedBranches()).isEmpty();
        assertThat(c.packageLpa()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void withIdOnlyChangesTheId() {
        Company c = company("Acme", "7", 1, 3, "CSE");
        Company saved = c.withId(42);
        assertThat(saved.id()).isEqualTo(42);
        assertThat(saved.name()).isEqualTo("Acme");
        assertThat(c.id()).isZero();
    }

    @Test
    void applicationStartsAppliedAndTracksStatus() {
        Application a = new Application(student("R1", "8.00", 0), company(5, "Acme", "7", 0, 1));
        assertThat(a.status()).isEqualTo(ApplicationStatus.APPLIED);
        a.setStatus(ApplicationStatus.SELECTED);
        assertThat(a.status()).isEqualTo(ApplicationStatus.SELECTED);
        assertThat(a.key()).isEqualTo(new ApplicationKey("R1", 5));
        assertThat(a.toString()).contains("R1", "Acme", "SELECTED");
    }

    @Test
    void applicationRejectsNulls() {
        assertThatThrownBy(() -> new Application(null, company("A", "7", 0, 1)))
                .isInstanceOf(NullPointerException.class);
        Application a = new Application(student("R1", "8.00", 0), company("A", "7", 0, 1));
        assertThatThrownBy(() -> a.setStatus(null)).isInstanceOf(NullPointerException.class);
    }
}
