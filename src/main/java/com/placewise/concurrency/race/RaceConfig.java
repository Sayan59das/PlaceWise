package com.placewise.concurrency.race;

/**
 * Settings of the race-condition demo.
 *
 * @param threads            competing threads, 2..64
 * @param companies          companies being competed for, 1..20
 * @param seatsPerCompany    seats each company has
 * @param attemptsPerThread  how many seats each thread tries to take
 * @param windowMillis       pause between "check" and "act" (0 = just yield)
 * @param seed               makes every thread pick the same companies in the unsafe and the safe run
 */
public record RaceConfig(int threads, int companies, int seatsPerCompany, int attemptsPerThread,
                         int windowMillis, long seed) {

    /** Checks every field. */
    public RaceConfig {
        if (threads < 2 || threads > 64) {
            throw new IllegalArgumentException("threads must be between 2 and 64");
        }
        if (companies < 1 || companies > 20) {
            throw new IllegalArgumentException("companies must be between 1 and 20");
        }
        if (seatsPerCompany < 1 || seatsPerCompany > 10_000) {
            throw new IllegalArgumentException("seats must be between 1 and 10000");
        }
        if (attemptsPerThread < 1) {
            throw new IllegalArgumentException("attempts must be at least 1");
        }
        if (windowMillis < 0 || windowMillis > 100) {
            throw new IllegalArgumentException("window must be between 0 and 100 ms");
        }
    }

    /**
     * Defaults chosen so that the race shows up on every run: 8 threads fight over 6 seats.
     *
     * @return the default configuration
     */
    public static RaceConfig defaults() {
        return new RaceConfig(8, 3, 2, 20, 5, 7L);
    }

    /**
     * Total seats on offer.
     *
     * @return companies times seats per company
     */
    public int totalSeats() {
        return companies * seatsPerCompany;
    }
}
