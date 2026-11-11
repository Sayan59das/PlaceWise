package com.placewise.concurrency.race;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.drive.DriveConfig;
import com.placewise.exception.NoSeatsException;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("concurrency")
@Timeout(120)
class RaceDemoTest {

    /** Two threads must both arrive before either may continue: forces the bad interleaving. */
    private static final class Rendezvous implements Runnable {
        private final int parties;
        private int arrived; // guarded by "this"

        Rendezvous(int parties) {
            this.parties = parties;
        }

        @Override
        public synchronized void run() {
            arrived++;
            notifyAll();
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (arrived < parties) {
                long left = (deadline - System.nanoTime()) / 1_000_000;
                if (left <= 0) {
                    throw new AssertionError("the other racer never arrived");
                }
                try {
                    wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Test
    void theUnsafeAllocatorOversellsDeterministicallyWhenTwoThreadsInterleave() throws InterruptedException {
        RaceConfig oneSeatTwoThreads = new RaceConfig(2, 1, 1, 1, 0, 1L);
        int[] granted = RaceDemo.runWorkload(new UnsafeSeatAllocator(new int[] {1}, new Rendezvous(2)),
                oneSeatTwoThreads);
        assertThat(granted[0]).as("seats handed out for ONE real seat").isEqualTo(2);
    }

    @Test
    void theSafeAllocatorNeverOversellsUnderTheSameTwoThreadWorkload() throws InterruptedException {
        RaceConfig oneSeatTwoThreads = new RaceConfig(2, 1, 1, 1, 0, 1L);
        int[] granted = RaceDemo.runWorkload(new SafeSeatAllocator(new int[] {1}, RaceDemo.pause(0)),
                oneSeatTwoThreads);
        assertThat(granted[0]).isEqualTo(1);
    }

    @Test
    void sameWorkloadUnsafeIsNotEqualToSafe() throws InterruptedException {
        RaceReport report = null;
        for (int attempt = 1; attempt <= 20; attempt++) { // the race is overwhelmingly likely; allow retries anyway
            report = RaceDemo.run(RaceConfig.defaults());
            assertThat(report.safeOversold()).as("safe allocator, attempt %d", attempt).isZero();
            assertThat(report.safeGranted()).as("every seat is sold exactly once").isEqualTo(6);
            if (report.unsafeOversold() > 0) {
                break;
            }
        }
        assertThat(report.unsafeOversold()).as("unsafe allocator oversold seats").isPositive();
        assertThat(report.unsafeGranted()).isGreaterThan(report.safeGranted());
        assertThat(report.lines()).hasSize(3);
        assertThat(report.lines().get(1)).startsWith("UNSAFE").contains("oversold " + report.unsafeOversold());
        assertThat(report.lines().get(2)).startsWith("SAFE").endsWith("oversold 0");
    }

    @Test
    void sixteenThreadsHammeringTheSafeAllocatorSellExactlyTheSeats() throws InterruptedException {
        RaceConfig config = new RaceConfig(16, 4, 25, 1_000, 0, 5L);
        SafeSeatAllocator safe = new SafeSeatAllocator(fill(4, 25), RaceDemo.pause(0));
        int[] granted = RaceDemo.runWorkload(safe, config);
        assertThat(granted).containsExactly(25, 25, 25, 25);
        for (int c = 0; c < 4; c++) {
            assertThat(safe.seatsLeft(c)).isZero();
        }
    }

    private static int[] fill(int companies, int seats) {
        int[] array = new int[companies];
        Arrays.fill(array, seats);
        return array;
    }

    @Test
    void allocateThrowsNoSeatsExceptionWhenFull() {
        SeatAllocator safe = new SafeSeatAllocator(new int[] {1}, RaceDemo.pause(0));
        safe.allocate(0, "Acme");
        assertThatThrownBy(() -> safe.allocate(0, "Acme")).isInstanceOf(NoSeatsException.class)
                .hasMessageContaining("Acme");
        SeatAllocator unsafe = new UnsafeSeatAllocator(new int[] {0}, RaceDemo.pause(0));
        assertThat(unsafe.tryAllocate(0)).isFalse();
    }

    @Test
    void pauseSleepsAtLeastTheRequestedTimeAndKeepsAnInterrupt() {
        long before = System.nanoTime();
        RaceDemo.pause(20).run();
        assertThat((System.nanoTime() - before) / 1_000_000).isGreaterThanOrEqualTo(15);
        RaceDemo.pause(0).run(); // just yields
        Thread.currentThread().interrupt();
        RaceDemo.pause(50).run();
        assertThat(Thread.interrupted()).as("interrupt flag restored, then cleared here").isTrue();
    }

    @ParameterizedTest
    @CsvSource({"1,3,2,20,5", "65,3,2,20,5", "8,0,2,20,5", "8,21,2,20,5", "8,3,0,20,5", "8,3,2,0,5", "8,3,2,20,-1",
            "8,3,2,20,101", "8,3,10001,20,5"})
    void invalidRaceConfigsAreRefused(int threads, int companies, int seats, int attempts, int window) {
        assertThatThrownBy(() -> new RaceConfig(threads, companies, seats, attempts, window, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalSeatsMultipliesCompaniesBySeats() {
        assertThat(RaceConfig.defaults().totalSeats()).isEqualTo(6);
    }

    @ParameterizedTest
    @CsvSource({"0,5,1,1,1,1,1,1", "1,0,1,1,1,1,1,1", "1,1,0,1,1,1,1,1", "1,1,1,0,1,1,1,1", "1,1,1,65,1,1,1,1",
            "1,1,1,1,0,1,1,1", "1,1,1,1,1,0,1,1", "1,1,1,1,1,1,101,1", "1,1,1,1,1,1,1,101", "100001,1,1,1,1,1,1,1",
            "1,1001,1,1,1,1,1,1", "1,1,100001,1,1,1,1,1"})
    void invalidDriveConfigsAreRefused(int students, int companies, int seats, int recruiters, int queue,
                                       int perStudent, int withdrawal, int duplicate) {
        assertThatThrownBy(() -> new DriveConfig(students, companies, seats, recruiters, 1L, queue, perStudent,
                withdrawal, duplicate)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsAreThe200StudentFiveCompanyFourRecruiterDemo() {
        DriveConfig defaults = DriveConfig.defaults();
        assertThat(defaults.students()).isEqualTo(200);
        assertThat(defaults.companies()).isEqualTo(5);
        assertThat(defaults.recruiters()).isEqualTo(4);
        assertThat(defaults.withSeed(5).seed()).isEqualTo(5);
        assertThat(defaults.withSeed(5).students()).isEqualTo(200);
    }
}
