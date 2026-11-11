package com.placewise.drive;

/**
 * Settings of a placement-drive simulation.
 *
 * @param students                 how many students to generate (ignored when real stored data is used)
 * @param companies                how many companies to generate (ignored for stored data)
 * @param seatsPerCompany          seats of each generated company (ignored for stored data)
 * @param recruiters               number of recruiter threads, 1..64
 * @param seed                     random seed; the same seed always plans the same applications
 * @param queueCapacity            size of the shared application queue (small values make producers wait)
 * @param maxApplicationsPerStudent each student applies to between 1 and this many companies
 * @param withdrawalPercent        chance (0..100) that a selected student later withdraws
 * @param duplicatePercent         chance (0..100) that a student submits one application twice
 */
public record DriveConfig(int students, int companies, int seatsPerCompany, int recruiters, long seed,
                          int queueCapacity, int maxApplicationsPerStudent, int withdrawalPercent,
                          int duplicatePercent) {

    /** Checks every field and fails early with a readable message. */
    public DriveConfig {
        require(students >= 1 && students <= 100_000, "students must be between 1 and 100000");
        require(companies >= 1 && companies <= 1_000, "companies must be between 1 and 1000");
        require(seatsPerCompany >= 1 && seatsPerCompany <= 100_000, "seats must be between 1 and 100000");
        require(recruiters >= 1 && recruiters <= 64, "recruiters must be between 1 and 64");
        require(queueCapacity >= 1, "queue capacity must be at least 1");
        require(maxApplicationsPerStudent >= 1, "applications per student must be at least 1");
        require(withdrawalPercent >= 0 && withdrawalPercent <= 100, "withdrawal percent must be 0..100");
        require(duplicatePercent >= 0 && duplicatePercent <= 100, "duplicate percent must be 0..100");
    }

    /**
     * The demo defaults: 200 students, 5 companies of 8 seats, 4 recruiters.
     *
     * @return the default configuration
     */
    public static DriveConfig defaults() {
        return new DriveConfig(200, 5, 8, 4, 42L, 16, 3, 20, 10);
    }

    /**
     * Copy with another seed.
     *
     * @param newSeed the seed
     * @return the new configuration
     */
    public DriveConfig withSeed(long newSeed) {
        return new DriveConfig(students, companies, seatsPerCompany, recruiters, newSeed, queueCapacity,
                maxApplicationsPerStudent, withdrawalPercent, duplicatePercent);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
