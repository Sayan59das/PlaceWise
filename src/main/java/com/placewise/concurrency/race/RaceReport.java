package com.placewise.concurrency.race;

import java.util.List;

/**
 * Result of running the same workload against both allocators.
 *
 * @param config          the workload
 * @param unsafeGranted   seats the unsafe allocator handed out
 * @param unsafeOversold  how many of those exceeded the real seats
 * @param safeGranted     seats the safe allocator handed out
 * @param safeOversold    oversold seats of the safe allocator (must be 0)
 */
public record RaceReport(RaceConfig config, int unsafeGranted, int unsafeOversold, int safeGranted,
                         int safeOversold) {

    /**
     * Console text.
     *
     * @return the workload and both outcomes
     */
    public List<String> lines() {
        int attempts = config.threads() * config.attemptsPerThread();
        return List.of(
                "Workload: " + config.threads() + " threads x " + config.attemptsPerThread() + " attempts = "
                        + attempts + " attempts for " + config.totalSeats() + " seats ("
                        + config.companies() + " companies x " + config.seatsPerCompany() + ")",
                "UNSAFE (no synchronization):  granted " + unsafeGranted + " seats -> oversold " + unsafeOversold,
                "SAFE   (lock per company):    granted " + safeGranted + " seats -> oversold " + safeOversold);
    }
}
