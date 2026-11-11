package com.placewise.ranking;

import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.model.Student;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StudentRankingTest {

    private static List<String> rolls(List<RankedStudent> ranked) {
        return ranked.stream().map(r -> r.student().rollNo()).toList();
    }

    @Test
    void ordersByCgpaDescending() {
        List<Student> input = List.of(student("A", "7.00", 0), student("B", "9.00", 0), student("C", "8.00", 0));
        assertThat(rolls(StudentRanking.rank(input))).containsExactly("B", "C", "A");
    }

    @Test
    void cgpaTieIsBrokenByFewerBacklogs() {
        List<Student> input = List.of(student("A", "8.00", 2), student("B", "8.00", 0), student("C", "8.00", 1));
        assertThat(rolls(StudentRanking.rank(input))).containsExactly("B", "C", "A");
    }

    @Test
    void thenByNameIgnoringCase() {
        List<Student> input = List.of(
                student("A", "zoya", "CSE", "8.00", 1),
                student("B", "Asha", "CSE", "8.00", 1),
                student("C", "bella", "CSE", "8.00", 1));
        assertThat(rolls(StudentRanking.rank(input))).containsExactly("B", "C", "A");
    }

    @Test
    void thenByRollNumber() {
        List<Student> input = List.of(
                student("R3", "Same", "CSE", "8.00", 1),
                student("R1", "Same", "CSE", "8.00", 1),
                student("R2", "same", "CSE", "8.00", 1));
        assertThat(rolls(StudentRanking.rank(input))).containsExactly("R1", "R2", "R3");
    }

    @Test
    void ranksAreConsecutiveFromOne() {
        List<Student> input = List.of(student("A", "8.00", 0), student("B", "8.00", 0), student("C", "8.00", 0));
        assertThat(StudentRanking.rank(input)).extracting(RankedStudent::rank).containsExactly(1, 2, 3);
    }

    @Test
    void outputIsIdenticalForOneHundredShuffles() {
        List<Student> base = new ArrayList<>();
        Random random = new Random(7);
        for (int i = 0; i < 60; i++) {
            // few distinct CGPAs/names on purpose, so most comparisons hit the tie-breakers
            String cgpa = String.valueOf(6 + random.nextInt(3)) + ".00";
            base.add(student("R" + String.format("%03d", i), "N" + random.nextInt(4), "CSE", cgpa,
                    random.nextInt(3)));
        }
        List<RankedStudent> expected = StudentRanking.rank(base);
        for (int round = 0; round < 100; round++) {
            List<Student> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, new Random(round));
            assertThat(StudentRanking.rank(shuffled)).isEqualTo(expected);
        }
    }

    @Test
    void emptyInputGivesEmptyOutputEverywhere() {
        assertThat(StudentRanking.rank(List.of())).isEmpty();
        assertThat(StudentRanking.top(List.of(), 5)).isEmpty();
        assertThat(StudentRanking.page(List.of(), 1, 10)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "3,3", "5,5", "6,5", "100,5"})
    void topNBoundaries(int n, int expectedSize) {
        List<Student> input = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            input.add(student("R" + i, "9." + (9 - i) + "0", 0));
        }
        List<RankedStudent> top = StudentRanking.top(input, n);
        assertThat(top).hasSize(expectedSize);
        if (expectedSize > 0) {
            assertThat(top.get(0).student().rollNo()).isEqualTo("R0");
        }
    }

    @Test
    void negativeTopIsRejected() {
        assertThatThrownBy(() -> StudentRanking.top(List.of(), -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paginationUsesSkipAndLimitAndKeepsGlobalRanks() {
        List<Student> input = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            input.add(student("R" + i, "9." + (9 - i) + "0", 0));
        }
        assertThat(rolls(StudentRanking.page(input, 1, 3))).containsExactly("R0", "R1", "R2");
        List<RankedStudent> second = StudentRanking.page(input, 2, 3);
        assertThat(rolls(second)).containsExactly("R3", "R4", "R5");
        assertThat(second).extracting(RankedStudent::rank).containsExactly(4, 5, 6);
        assertThat(rolls(StudentRanking.page(input, 3, 3))).containsExactly("R6");
        assertThat(StudentRanking.page(input, 4, 3)).isEmpty();
        assertThat(rolls(StudentRanking.page(input, 1, 100))).hasSize(7);
    }

    @Test
    void badPaginationArgumentsAreRejected() {
        assertThatThrownBy(() -> StudentRanking.page(List.of(), 0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StudentRanking.page(List.of(), 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hugePageNumberDoesNotOverflow() {
        assertThat(StudentRanking.page(List.of(student("A", "8", 0)), Integer.MAX_VALUE, Integer.MAX_VALUE))
                .isEmpty();
    }

    @Test
    void comparatorIsAntisymmetricAndTransitiveOnTies() {
        Student a = student("A", "N", "CSE", "8.00", 1);
        Student b = student("B", "n", "CSE", "8.00", 1);
        assertThat(StudentRanking.ORDER.compare(a, b)).isNegative();
        assertThat(StudentRanking.ORDER.compare(b, a)).isPositive();
        assertThat(StudentRanking.ORDER.compare(a, a)).isZero();
    }

    // ---- StudentSort -----------------------------------------------------------------------------

    @Test
    void sortFieldsOrderAsDocumented() {
        Student a = new Student("R1", "zed", "IT", 3, new java.math.BigDecimal("7"), 2, null);
        Student b = new Student("R2", "Amy", "CSE", 8, new java.math.BigDecimal("9"), 0, null);
        Student c = new Student("R3", "bob", "ECE", 5, new java.math.BigDecimal("8"), 1, null);
        List<Student> all = List.of(c, a, b);
        assertThat(sorted(all, StudentSort.ROLL_NO)).containsExactly("R1", "R2", "R3");
        assertThat(sorted(all, StudentSort.NAME)).containsExactly("R2", "R3", "R1");
        assertThat(sorted(all, StudentSort.BRANCH)).containsExactly("R2", "R3", "R1");
        assertThat(sorted(all, StudentSort.SEMESTER)).containsExactly("R1", "R3", "R2");
        assertThat(sorted(all, StudentSort.CGPA)).containsExactly("R2", "R3", "R1");
        assertThat(sorted(all, StudentSort.BACKLOGS)).containsExactly("R2", "R3", "R1");
    }

    private static List<String> sorted(List<Student> students, StudentSort sort) {
        return students.stream().sorted(sort.comparator()).map(Student::rollNo).toList();
    }

    @Test
    void sortParseAcceptsFriendlyText() {
        assertThat(StudentSort.parse("cgpa")).contains(StudentSort.CGPA);
        assertThat(StudentSort.parse(" roll no ")).contains(StudentSort.ROLL_NO);
        assertThat(StudentSort.parse("height")).isEmpty();
        assertThat(StudentSort.parse(null)).isEmpty();
    }
}
