package com.placewise.drive;

import static com.placewise.drive.DriveFixtures.CHECKER;
import static com.placewise.drive.DriveFixtures.drive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.Await;
import com.placewise.audit.AuditEvent;
import com.placewise.audit.AuditLog;
import com.placewise.concurrency.BoundedQueue;
import com.placewise.exception.DriveException;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("concurrency")
class PlacementDriveTest {

    private static void assertInvariants(DriveResult result) {
        assertThat(DriveInvariants.check(result, CHECKER)).as("invariant violations").isEmpty();
        // the boards must hold exactly what a naive "top seats by rank" computation predicts
        Map<Long, List<Set<String>>> expected = DriveFixtures.expectedAllocation(result);
        for (BoardSnapshot board : result.boards()) {
            long id = board.company().id();
            assertThat(DriveFixtures.rolls(board.selected())).as("selected at %s", board.company().name())
                    .isEqualTo(expected.get(id).get(0));
            assertThat(DriveFixtures.rolls(board.waitlisted())).as("waitlisted at %s", board.company().name())
                    .isEqualTo(expected.get(id).get(1));
        }
    }

    @RepeatedTest(50)
    @Timeout(30)
    void twoHundredStudentsFiveCompaniesFourRecruitersKeepEveryInvariant(RepetitionInfo info) {
        DriveConfig config = DriveConfig.defaults().withSeed(info.getCurrentRepetition());
        DriveResult result = drive(config, AuditLog.none());

        assertThat(result.boards()).hasSize(5);
        assertThat(result.applications()).isNotEmpty();
        assertInvariants(result);
        for (BoardSnapshot board : result.boards()) {
            assertThat(board.selected().size()).as("seats used at %s", board.company().name())
                    .isLessThanOrEqualTo(8);
        }
        // every application ends in exactly one final status, and all five non-initial statuses add up
        long total = 0;
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status != ApplicationStatus.APPLIED) {
                total += result.count(status);
            }
        }
        assertThat(result.count(ApplicationStatus.APPLIED)).isZero();
        assertThat(total).isEqualTo(result.applications().size());
    }

    @Test
    @Timeout(60)
    void theSameSeedGivesTheSameOutcomeEvenThoughThreadTimingDiffers() {
        DriveConfig config = DriveConfig.defaults().withSeed(99);
        Map<String, String> first = signature(drive(config, AuditLog.none()));
        for (int run = 0; run < 10; run++) {
            assertThat(signature(drive(config, AuditLog.none()))).isEqualTo(first);
        }
    }

    /** student->company to the status of its live application (duplicates always end REJECTED). */
    private static Map<String, String> signature(DriveResult result) {
        Map<String, String> signature = new TreeMap<>();
        for (Application a : result.applications()) {
            String key = a.student().rollNo() + "@" + a.company().name();
            if (a.status() != ApplicationStatus.REJECTED || !signature.containsKey(key)) {
                signature.merge(key, a.status().name(), (old, now) -> old.equals("REJECTED") ? now : old);
            }
        }
        return signature;
    }

    @Test
    @Timeout(60)
    void everyWithdrawalOfASelectedStudentPromotesSomeoneFromTheWaitlist() {
        DriveConfig config = new DriveConfig(120, 3, 5, 4, 5L, 8, 3, 100, 0);
        DriveResult result = drive(config, AuditLog.none());
        assertInvariants(result);
        assertThat(result.count(ApplicationStatus.WITHDRAWN)).isEqualTo(15); // all 3 x 5 first-round winners left
        for (BoardSnapshot board : result.boards()) {
            assertThat(board.selected()).hasSize(5); // refilled from the waitlist
            assertThat(board.selected()).allMatch(a -> a.status() == ApplicationStatus.SELECTED);
        }
    }

    @Test
    @Timeout(60)
    void withNoWithdrawalsNobodyIsPromotedOrWithdrawn() {
        DriveResult result = drive(new DriveConfig(150, 4, 6, 3, 8L, 4, 2, 0, 20), AuditLog.none());
        assertInvariants(result);
        assertThat(result.count(ApplicationStatus.WITHDRAWN)).isZero();
    }

    @Test
    @Timeout(60)
    void duplicateApplicationsAreRejectedAndNeverSelectedTwice() {
        DriveResult result = drive(new DriveConfig(100, 3, 10, 4, 3L, 8, 3, 0, 100), AuditLog.none());
        assertInvariants(result);
        // every student submitted one duplicate, so there are rejected duplicates among eligible students
        long duplicates = result.applications().stream().filter(a -> a.status() == ApplicationStatus.REJECTED)
                .filter(a -> CHECKER.isEligible(a.student(), a.company())).count();
        assertThat(duplicates).isPositive();
    }

    @Test
    @Timeout(60)
    void aQueueOfOneAndASingleRecruiterStillFinishes() {
        assertInvariants(drive(new DriveConfig(80, 3, 4, 1, 1L, 1, 3, 30, 10), AuditLog.none()));
    }

    @Test
    @Timeout(60)
    void manyRecruitersAndMoreStudentsThanThreadsStillAgreeWithTheReference() {
        DriveConfig config = new DriveConfig(700, 6, 12, 32, 77L, 64, 4, 25, 10);
        DriveResult result = drive(config, AuditLog.none());
        assertInvariants(result);
        assertThat(PlacementDrive.MAX_STUDENT_THREADS).isLessThan(config.students());
    }

    @Test
    @Timeout(60)
    void aSingleStudentAndCompanyWorks() {
        DriveResult result = drive(new DriveConfig(1, 1, 1, 1, 4L, 1, 1, 0, 0), AuditLog.none());
        assertInvariants(result);
        assertThat(result.applications()).hasSize(1);
    }

    @Test
    @Timeout(30)
    void noStudentsOrNoCompaniesIsAnEmptyButValidResult() {
        PlacementDrive none = new PlacementDrive(List.of(), List.of(), DriveConfig.defaults(), CHECKER,
                AuditLog.none(), null);
        DriveResult result = none.run();
        assertThat(result.applications()).isEmpty();
        assertThat(result.summaryLines().get(result.summaryLines().size() - 2)).contains("Applications: 0");
        List<Student> students = DriveDataGenerator.students(5, 1);
        assertThat(new PlacementDrive(students, List.of(), DriveConfig.defaults(), CHECKER, AuditLog.none(), null)
                .run().applications()).isEmpty();
    }

    @Test
    void companyIdsMustBeDistinct() {
        Company a = DriveDataGenerator.companies(1, 3, 1).get(0);
        assertThatThrownBy(() -> new PlacementDrive(List.of(), List.of(a, a), DriveConfig.defaults(), CHECKER,
                AuditLog.none(), null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("distinct");
    }

    @Test
    @Timeout(60)
    void theAuditLogRecordsEveryStepWithTheRightThreadNames() {
        DriveFixtures.RecordingAudit audit = new DriveFixtures.RecordingAudit();
        DriveResult result = drive(DriveConfig.defaults().withSeed(12), audit);
        int applications = result.applications().size();
        assertThat(audit.count(AuditEvent.APPLICATION)).isEqualTo(applications);
        assertThat(audit.count(AuditEvent.ELIGIBILITY_CHECK)).isEqualTo(applications);
        assertThat(audit.count(AuditEvent.WITHDRAWN)).isEqualTo(result.count(ApplicationStatus.WITHDRAWN));
        assertThat(audit.count(AuditEvent.SELECTED)).isGreaterThanOrEqualTo(result.count(ApplicationStatus.SELECTED));
        List<String> names = audit.lines.stream().map(l -> l.split("\\|")[1]).distinct().toList();
        assertThat(names).anyMatch(n -> n.startsWith("student-")).anyMatch(n -> n.startsWith("recruiter-"));
        assertThat(names).allMatch(n -> n.startsWith("student-") || n.startsWith("recruiter-")
                || n.startsWith("withdraw-"));
    }

    @Test
    @Timeout(60)
    void aFailingRecruiterAbortsTheDriveCleanlyWithoutHangingOrLeakingThreads() {
        AuditLog bomb = (event, message) -> {
            if (event == AuditEvent.SELECTED) {
                throw new IllegalStateException("audit down");
            }
        };
        assertThatThrownBy(() -> drive(DriveConfig.defaults(), bomb)).isInstanceOf(DriveException.class)
                .hasMessageContaining("failed").hasRootCauseMessage("audit down");
        assertNoWorkerThreadsAlive();
    }

    @Test
    @Timeout(60)
    void aFailingStudentThreadAbortsTheDriveCleanlyToo() {
        AuditLog bomb = (event, message) -> {
            if (event == AuditEvent.APPLICATION) {
                throw new IllegalStateException("cannot log applications");
            }
        };
        assertThatThrownBy(() -> drive(DriveConfig.defaults(), bomb)).isInstanceOf(DriveException.class);
        assertNoWorkerThreadsAlive();
    }

    private static void assertNoWorkerThreadsAlive() {
        Await.until(() -> Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.isAlive()
                && (t.getName().startsWith("recruiter-") || t.getName().startsWith("student-")
                || t.getName().startsWith("withdraw-") || t.getName().equals("progress-monitor"))),
                "all drive threads to be gone");
    }

    @Test
    @Timeout(60)
    void interruptingTheCallerStopsTheDriveAndKeepsTheInterruptFlag() {
        // a slow audit log keeps the drive busy long enough to be interrupted in the middle
        AuditLog slow = (event, message) -> {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicReference<Boolean> flagAfter = new AtomicReference<>();
        Thread driver = new Thread(() -> {
            try {
                drive(new DriveConfig(300, 5, 8, 2, 1L, 2, 3, 20, 10), slow);
            } catch (Throwable t) {
                thrown.set(t);
            }
            flagAfter.set(Thread.currentThread().isInterrupted());
        }, "driver");
        driver.start();
        Await.until(() -> Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().startsWith("recruiter-")),
                "the drive to be running");
        driver.interrupt();
        Await.finished(driver);
        assertThat(thrown.get()).isInstanceOf(DriveException.class).hasMessageContaining("interrupted")
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(flagAfter.get()).as("interrupt flag restored for the caller").isTrue();
        assertNoWorkerThreadsAlive();
    }

    @Test
    @Timeout(30)
    void theProgressMonitorIsADaemonThatPrintsAndStopsOnRequest() throws InterruptedException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true);
        ProgressCounter counter = new ProgressCounter();
        counter.submitted();
        counter.submitted();
        counter.processed();
        ProgressMonitor monitor = new ProgressMonitor(out, counter, new com.placewise.concurrency.BoundedQueue<String>(4), 9, 5);
        Thread thread = monitor.start();
        assertThat(thread.isDaemon()).as("daemon threads do not keep the JVM alive").isTrue();
        Await.until(() -> bytes.size() > 0, "a progress line");
        monitor.stop();
        assertThat(thread.isAlive()).isFalse();
        assertThat(bytes.toString()).contains("submitted 2, processed 1 of 9 applications, queue 0/4");
        assertThat(counter.submittedCount()).isEqualTo(2);
        assertThat(counter.processedCount()).isEqualTo(1);
    }

    @Test
    @Timeout(30)
    void aDriveWithAProgressStreamStillCompletesAndLeavesNoMonitorBehind() {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true);
        List<Student> students = DriveDataGenerator.students(50, 2);
        List<Company> companies = DriveDataGenerator.companies(3, 4, 2);
        DriveResult result = new PlacementDrive(students, companies, DriveConfig.defaults(), CHECKER,
                AuditLog.none(), sink).run();
        assertThat(DriveInvariants.check(result, CHECKER)).isEmpty();
        assertNoWorkerThreadsAlive();
    }

    @Test
    void summaryLinesDescribeEachCompanyAndTheTotals() {
        DriveResult result = drive(DriveConfig.defaults().withSeed(3), AuditLog.none());
        List<String> lines = result.summaryLines();
        assertThat(lines).hasSize(result.companies().size() + 2);
        assertThat(lines.get(0)).contains("seats").contains("selected").contains("waitlisted");
        assertThat(lines.get(lines.size() - 1)).contains("Students placed: " + result.placedStudents());
        assertThat(result.placedStudents()).isEqualTo(result.applications().stream()
                .filter(a -> a.status() == ApplicationStatus.SELECTED).map(a -> a.student().rollNo())
                .collect(Collectors.toSet()).size());
    }
}
