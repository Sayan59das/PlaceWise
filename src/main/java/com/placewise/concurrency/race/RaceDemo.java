package com.placewise.concurrency.race;

import com.placewise.concurrency.StartGate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Shows a race condition and its cure side by side: the <em>same</em> workload (same threads, same
 * random company choices) is run against {@link UnsafeSeatAllocator} and {@link SafeSeatAllocator}.
 */
public final class RaceDemo {

    private RaceDemo() {
    }

    /**
     * Runs the comparison.
     *
     * @param config the workload
     * @return how many seats each allocator sold and how many were oversold
     * @throws InterruptedException if the calling thread is interrupted while waiting for the racers
     */
    public static RaceReport run(RaceConfig config) throws InterruptedException {
        int[] seats = new int[config.companies()];
        Arrays.fill(seats, config.seatsPerCompany());
        Runnable window = pause(config.windowMillis());
        int[] unsafe = runWorkload(new UnsafeSeatAllocator(seats, window), config);
        int[] safe = runWorkload(new SafeSeatAllocator(seats, window), config);
        return new RaceReport(config, sum(unsafe), oversold(unsafe, config.seatsPerCompany()), sum(safe),
                oversold(safe, config.seatsPerCompany()));
    }

    /**
     * Lets many threads fight for seats and counts what each company handed out. Every thread writes
     * only to its own row of the result table, so the counting itself is race-free; the outcome depends
     * only on the allocator under test.
     *
     * @param allocator the allocator to test
     * @param config    the workload
     * @return seats granted per company (index = company)
     * @throws InterruptedException if interrupted while waiting for the threads
     */
    public static int[] runWorkload(SeatAllocator allocator, RaceConfig config) throws InterruptedException {
        int[][] grantedByThread = new int[config.threads()][config.companies()];
        StartGate gate = new StartGate();
        List<Thread> racers = new ArrayList<>();
        for (int t = 0; t < config.threads(); t++) {
            int id = t;
            Thread racer = new Thread(() -> {
                try {
                    gate.awaitOpen(); // everybody starts together, so they really collide
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Random random = new Random(config.seed() + id);
                for (int attempt = 0; attempt < config.attemptsPerThread(); attempt++) {
                    int company = random.nextInt(config.companies());
                    if (allocator.tryAllocate(company)) {
                        grantedByThread[id][company]++;
                    }
                }
            }, "racer-" + (t + 1));
            racers.add(racer);
            racer.start();
        }
        try {
            gate.open();
            for (Thread racer : racers) {
                racer.join(); // after join() the main thread safely sees every racer's writes
            }
        } catch (InterruptedException e) {
            for (Thread racer : racers) {
                racer.interrupt();
            }
            throw e;
        }
        int[] granted = new int[config.companies()];
        for (int[] row : grantedByThread) {
            for (int c = 0; c < granted.length; c++) {
                granted[c] += row[c];
            }
        }
        return granted;
    }

    /**
     * Creates the "slow work between check and act".
     *
     * @param millis how long to sleep; 0 only yields the processor
     * @return a runnable that pauses
     */
    public static Runnable pause(int millis) {
        return () -> {
            if (millis <= 0) {
                Thread.yield();
                return;
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private static int sum(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }

    private static int oversold(int[] granted, int seatsPerCompany) {
        int oversold = 0;
        for (int seatsGranted : granted) {
            oversold += Math.max(0, seatsGranted - seatsPerCompany); // seats given away beyond the real number
        }
        return oversold;
    }
}
