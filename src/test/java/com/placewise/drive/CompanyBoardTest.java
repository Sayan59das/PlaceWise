package com.placewise.drive;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.audit.AuditEvent;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentRanking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(60)
class CompanyBoardTest {

    private final Company acme = company(1, "Acme", "6.00", 5, 2);
    private final CompanyBoard board = new CompanyBoard(acme);

    private Application app(String roll, String cgpa) {
        return new Application(student(roll, cgpa, 0), acme);
    }

    @Test
    void freeSeatsAreTakenFirstComeFirstServed() {
        Application a = app("A", "7.00");
        Application b = app("B", "8.00");
        assertThat(board.admit(a)).extracting(Decision::event).containsExactly(AuditEvent.SELECTED);
        assertThat(board.admit(b)).extracting(Decision::event).containsExactly(AuditEvent.SELECTED);
        assertThat(a.status()).isEqualTo(ApplicationStatus.SELECTED);
        assertThat(board.selectedCount()).isEqualTo(2);
    }

    @Test
    void aWorseApplicantAfterTheSeatsAreFullIsWaitlisted() {
        board.admit(app("A", "9.00"));
        board.admit(app("B", "8.00"));
        Application late = app("C", "7.00");
        assertThat(board.admit(late)).extracting(Decision::event).containsExactly(AuditEvent.WAITLISTED);
        assertThat(late.status()).isEqualTo(ApplicationStatus.WAITLISTED);
        assertThat(board.snapshot().waitlisted()).containsExactly(late);
    }

    @Test
    void aBetterApplicantDisplacesTheWorstSelectedOne() {
        Application worst = app("A", "7.00");
        Application mid = app("B", "8.00");
        board.admit(worst);
        board.admit(mid);
        Application star = app("C", "9.00");
        List<Decision> decisions = board.admit(star);
        assertThat(decisions).extracting(Decision::event).containsExactly(AuditEvent.WAITLISTED, AuditEvent.SELECTED);
        assertThat(decisions.get(0).application()).isSameAs(worst);
        assertThat(decisions.get(0).message()).contains("A -> Acme", "displaced by better-ranked C");
        assertThat(worst.status()).isEqualTo(ApplicationStatus.WAITLISTED);
        assertThat(star.status()).isEqualTo(ApplicationStatus.SELECTED);
        assertThat(board.snapshot().selected()).containsExactly(star, mid); // best first
    }

    @Test
    void anEqualOrWorseNewcomerNeverDisplacesAnIncumbent() {
        board.admit(app("A", "8.00"));
        board.admit(app("B", "8.00"));
        Application tieButLaterRoll = app("C", "8.00");
        board.admit(tieButLaterRoll);
        assertThat(tieButLaterRoll.status()).isEqualTo(ApplicationStatus.WAITLISTED);
    }

    @Test
    void theOutcomeDoesNotDependOnArrivalOrder() {
        List<Student> students = new ArrayList<>();
        Random random = new Random(1);
        for (int i = 0; i < 30; i++) {
            students.add(student("R" + String.format("%02d", i), "6." + (10 + random.nextInt(8) * 5), random.nextInt(3)));
        }
        Company c = company(1, "Acme", "6.00", 5, 7);
        Set<String> expected = new TreeSet<>();
        students.stream().sorted(StudentRanking.ORDER).limit(7).forEach(s -> expected.add(s.rollNo()));
        for (int round = 0; round < 100; round++) {
            List<Student> shuffled = new ArrayList<>(students);
            Collections.shuffle(shuffled, new Random(round));
            CompanyBoard fresh = new CompanyBoard(c);
            shuffled.forEach(s -> fresh.admit(new Application(s, c)));
            assertThat(DriveFixtures.rolls(fresh.snapshot().selected())).isEqualTo(expected);
            assertThat(fresh.snapshot().waitlisted()).hasSize(23);
        }
    }

    @Test
    void aWithdrawalPromotesTheBestWaitlistedApplicant() {
        Application a = app("A", "9.00");
        Application b = app("B", "8.00");
        Application c = app("C", "7.50");
        Application d = app("D", "7.00");
        List.of(a, b, c, d).forEach(board::admit);
        List<Decision> decisions = board.withdraw(a);
        assertThat(decisions).extracting(Decision::event).containsExactly(AuditEvent.WITHDRAWN, AuditEvent.SELECTED);
        assertThat(decisions.get(1).message()).contains("promoted from waitlist after withdrawal by A");
        assertThat(a.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(c.status()).isEqualTo(ApplicationStatus.SELECTED);
        assertThat(d.status()).isEqualTo(ApplicationStatus.WAITLISTED);
        assertThat(board.selectedCount()).isEqualTo(2);
    }

    @Test
    void withdrawingWithNoWaitlistJustFreesTheSeat() {
        Application a = app("A", "9.00");
        board.admit(a);
        assertThat(board.withdraw(a)).extracting(Decision::event).containsExactly(AuditEvent.WITHDRAWN);
        assertThat(board.selectedCount()).isZero();
        Application late = app("B", "5.00");
        board.admit(late);
        assertThat(late.status()).isEqualTo(ApplicationStatus.SELECTED); // the freed seat is usable again
    }

    @Test
    void aWaitlistedStudentMayLeaveTheWaitlist() {
        Application a = app("A", "9.00");
        Application b = app("B", "8.00");
        Application c = app("C", "7.00");
        List.of(a, b, c).forEach(board::admit);
        assertThat(board.withdraw(c)).extracting(Decision::event).containsExactly(AuditEvent.WITHDRAWN);
        assertThat(c.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(board.snapshot().waitlisted()).isEmpty();
        assertThat(board.selectedCount()).isEqualTo(2);
    }

    @Test
    void withdrawingSomeoneWhoHoldsNothingChangesNothing() {
        Application stranger = app("Z", "9.00");
        assertThat(board.withdraw(stranger)).isEmpty();
        assertThat(stranger.status()).isEqualTo(ApplicationStatus.APPLIED);
        Application a = app("A", "8.00");
        board.admit(a);
        board.withdraw(a);
        assertThat(board.withdraw(a)).isEmpty(); // second withdrawal of the same application is a no-op
    }

    @Test
    void aSecondApplicationFromTheSameStudentIsRejectedAsDuplicate() {
        Application first = app("A", "9.00");
        Application second = app("A", "9.00");
        board.admit(first);
        List<Decision> decisions = board.admit(second);
        assertThat(decisions).extracting(Decision::event).containsExactly(AuditEvent.REJECTED);
        assertThat(decisions.get(0).message()).contains("duplicate application");
        assertThat(second.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(first.status()).isEqualTo(ApplicationStatus.SELECTED);
        assertThat(board.selectedCount()).isEqualTo(1);
    }

    @Test
    void anApplicationForAnotherCompanyIsRefused() {
        Application wrong = new Application(student("A", "9", 0), company(2, "Other", "6", 5, 1));
        assertThatThrownBy(() -> board.admit(wrong)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another company");
    }

    @Test
    void aCompanyWithNoSeatsOnlyWaitlists() {
        CompanyBoard empty = new CompanyBoard(company(3, "Empty", "6", 5, 0));
        Application a = new Application(student("A", "9", 0), company(3, "Empty", "6", 5, 0));
        assertThat(empty.admit(a)).extracting(Decision::event).containsExactly(AuditEvent.WAITLISTED);
        assertThat(empty.selectedCount()).isZero();
        assertThat(empty.company().name()).isEqualTo("Empty");
    }

    @Tag("concurrency")
    @Test
    void eightThreadsAdmittingAtOnceStillYieldExactlyTheTopRankedSeatHolders() throws InterruptedException {
        Company c = company(1, "Acme", "0", 9, 25);
        CompanyBoard shared = new CompanyBoard(c);
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 4_000; i++) {
            students.add(student("S" + String.format("%04d", i), "5." + (i % 100 < 10 ? "0" : "") + (i % 100), i % 3));
        }
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int id = t;
            Thread thread = new Thread(() -> {
                for (int i = id; i < students.size(); i += 8) {
                    shared.admit(new Application(students.get(i), c));
                }
            }, "admit-" + t);
            threads.add(thread);
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        Set<String> expected = new TreeSet<>();
        students.stream().sorted(StudentRanking.ORDER).limit(25).forEach(s -> expected.add(s.rollNo()));
        BoardSnapshot snapshot = shared.snapshot();
        assertThat(DriveFixtures.rolls(snapshot.selected())).isEqualTo(expected);
        assertThat(snapshot.selected()).hasSize(25);
        assertThat(snapshot.waitlisted()).hasSize(3_975);
    }
}
