package com.placewise.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentRanking;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Compares the real, stream-based implementation with a deliberately naive one that uses only loops and
 * integer arithmetic on hundredths. If the two ever disagree, one of them has a bug.
 */
class ReferenceModelTest {

    private static final String[] BRANCHES = {"CSE", "IT", "ECE", "ME", "CE"};
    private static final String[] NAMES = {"Asha", "asha", "Bela", "Chirag", "Dev", "dev", "Esha", "Farah",
            "Gita", "Hari", "Ira", "Jay", "Kabir", "Lata", "Meera", "Neel", "Om", "Pia", "Quin", "Ravi"};

    // ---- naive reference implementation (no streams, no comparators, no BigDecimal.compareTo) ------

    private static int hundredths(BigDecimal value) {
        return value.movePointRight(2).intValueExact();
    }

    private static boolean naiveEligible(Student s, Company c) {
        if (hundredths(s.cgpa()) < hundredths(c.minCgpa())) {
            return false;
        }
        if (s.activeBacklogs() > c.maxBacklogs()) {
            return false;
        }
        if (c.allowedBranches().isEmpty()) {
            return true;
        }
        for (String branch : c.allowedBranches()) {
            if (branch.equals(s.branch())) {
                return true;
            }
        }
        return false;
    }

    private static int naiveFailureCount(Student s, Company c) {
        int failures = 0;
        if (hundredths(s.cgpa()) < hundredths(c.minCgpa())) {
            failures++;
        }
        if (s.activeBacklogs() > c.maxBacklogs()) {
            failures++;
        }
        if (!c.allowedBranches().isEmpty() && !c.allowedBranches().contains(s.branch())) {
            failures++;
        }
        return failures;
    }

    /** True when {@code a} (CGPA {@code ca} in hundredths) must be listed before {@code b}. */
    private static boolean naiveBetter(Student a, int ca, Student b, int cb) {
        if (ca != cb) {
            return ca > cb;
        }
        if (a.activeBacklogs() != b.activeBacklogs()) {
            return a.activeBacklogs() < b.activeBacklogs();
        }
        int byName = a.name().toLowerCase().compareTo(b.name().toLowerCase());
        if (byName != 0) {
            return byName < 0;
        }
        return a.rollNo().compareTo(b.rollNo()) < 0;
    }

    /** Position = number of students that beat me. O(n^2) but obviously correct. */
    private static Student[] naiveGlobalOrder(List<Student> students) {
        int n = students.size();
        int[] cgpa = new int[n]; // converted once, so the n*n loop below allocates nothing
        for (int i = 0; i < n; i++) {
            cgpa[i] = hundredths(students.get(i).cgpa());
        }
        Student[] ordered = new Student[n];
        for (int i = 0; i < n; i++) {
            int beatenBy = 0;
            for (int j = 0; j < n; j++) {
                if (j != i && naiveBetter(students.get(j), cgpa[j], students.get(i), cgpa[i])) {
                    beatenBy++;
                }
            }
            assertThat(ordered[beatenBy]).as("positions must be unique (strict total order)").isNull();
            ordered[beatenBy] = students.get(i);
        }
        return ordered;
    }

    // ---- random data ----------------------------------------------------------------------------

    private static List<Student> randomStudents(Random random, int count) {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // CGPA in steps of 0.25 so many students tie and the tie-breakers are exercised constantly
            BigDecimal cgpa = BigDecimal.valueOf(500 + 25 * random.nextInt(21), 2);
            students.add(new Student(String.format("R%05d", i), NAMES[random.nextInt(NAMES.length)],
                    BRANCHES[random.nextInt(BRANCHES.length)], 1 + random.nextInt(8), cgpa,
                    random.nextInt(5), null));
        }
        return students;
    }

    private static List<Company> randomCompanies(Random random, int count) {
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Set<String> branches = new HashSet<>();
            int howMany = random.nextInt(4); // 0 means "all branches"
            for (int b = 0; b < howMany; b++) {
                branches.add(BRANCHES[random.nextInt(BRANCHES.length)]);
            }
            companies.add(new Company(i + 1, "Company" + i, BigDecimal.valueOf(550 + 25 * random.nextInt(15), 2),
                    random.nextInt(4), branches, BigDecimal.TEN, 5));
        }
        return companies;
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 42, 2026})
    void productionAndNaiveImplementationsAgree(long seed) {
        Random random = new Random(seed);
        List<Student> students = randomStudents(random, 5_000);
        List<Company> companies = randomCompanies(random, 20);
        EligibilityChecker checker = new EligibilityChecker();

        Student[] naiveOrder = naiveGlobalOrder(students);
        List<Student> shuffled = new ArrayList<>(students);
        Collections.shuffle(shuffled, new Random(seed + 1)); // production must not depend on input order

        for (Company company : companies) {
            List<String> expected = new ArrayList<>();
            for (Student s : naiveOrder) {
                if (naiveEligible(s, company)) {
                    expected.add(s.rollNo());
                }
            }
            List<Student> eligible = checker.eligibleStudents(company, shuffled);
            List<String> actual = StudentRanking.rank(eligible).stream().map(r -> r.student().rollNo()).toList();
            assertThat(actual).as("ranked eligible list of %s, seed %d", company.name(), seed)
                    .isEqualTo(expected);

            for (Student s : students) {
                EligibilityResult result = checker.check(s, company);
                assertThat(result.eligible()).isEqualTo(naiveEligible(s, company));
                assertThat(result.failedReasons()).hasSize(naiveFailureCount(s, company));
            }
        }
    }
}
