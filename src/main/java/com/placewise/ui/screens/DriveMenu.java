package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.concurrency.ThreadLifecycleDemo;
import com.placewise.concurrency.race.RaceConfig;
import com.placewise.concurrency.race.RaceDemo;
import com.placewise.concurrency.race.RaceReport;
import com.placewise.drive.DriveConfig;
import com.placewise.exception.DriveException;
import com.placewise.service.DriveOutcome;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import java.util.List;

/** Screen 7: the multithreaded placement drive, the race-condition demo and the thread-lifecycle demo. */
public final class DriveMenu extends Menu {

    private static final int RACE_ATTEMPTS = 5;

    public DriveMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Placement drive simulation", List.of("Run a drive on generated data",
                    "Run a drive on the stored students and companies", "Race-condition demo (unsafe vs safe)",
                    "Thread lifecycle demo"));
            switch (choice) {
                case 1 -> generated();
                case 2 -> stored();
                case 3 -> race();
                case 4 -> lifecycle();
                default -> { }
            }
        } while (choice != 0);
    }

    private void generated() {
        DriveConfig defaults = DriveConfig.defaults();
        int students = io.integer("Students", 1, 100_000, defaults.students());
        int companies = io.integer("Companies", 1, 1_000, defaults.companies());
        int seats = io.integer("Seats per company", 1, 100_000, defaults.seatsPerCompany());
        int recruiters = io.integer("Recruiter threads", 1, 64, defaults.recruiters());
        long seed = io.number("Random seed", defaults.seed());
        DriveConfig config = new DriveConfig(students, companies, seats, recruiters, seed, defaults.queueCapacity(),
                defaults.maxApplicationsPerStudent(), defaults.withdrawalPercent(), defaults.duplicatePercent());
        io.println("Starting: " + Math.min(students, 256) + " student thread(s), " + recruiters
                + " recruiter thread(s), shared queue of " + config.queueCapacity() + " ...");
        show(app.drives().runGenerated(config, io.out()));
    }

    private void stored() {
        DriveConfig defaults = DriveConfig.defaults();
        int recruiters = io.integer("Recruiter threads", 1, 64, defaults.recruiters());
        long seed = io.number("Random seed", defaults.seed());
        DriveConfig config = new DriveConfig(1, 1, 1, recruiters, seed, defaults.queueCapacity(),
                defaults.maxApplicationsPerStudent(), defaults.withdrawalPercent(), defaults.duplicatePercent());
        show(app.drives().runStored(config, io.out()));
    }

    private void show(DriveOutcome outcome) {
        io.blank();
        for (String line : outcome.result().summaryLines()) {
            io.println(line);
        }
        if (outcome.violations().isEmpty()) {
            io.println("Invariants: ALL HOLD (no company over its seats, nobody selected twice, every application "
                    + "has one final status, only eligible students selected, waitlist never outranks selected).");
        } else {
            io.println("INVARIANT VIOLATIONS:");
            for (String violation : outcome.violations()) {
                io.println("  - " + violation);
            }
        }
        io.println(outcome.persistence());
    }

    private void race() {
        RaceConfig defaults = RaceConfig.defaults();
        int threads = io.integer("Competing threads", 2, 64, defaults.threads());
        int seats = io.integer("Seats per company", 1, 10_000, defaults.seatsPerCompany());
        int window = io.integer("Pause between check and update, in ms", 0, 100, defaults.windowMillis());
        RaceConfig config = new RaceConfig(threads, defaults.companies(), seats, defaults.attemptsPerThread(), window,
                defaults.seed());
        try {
            RaceReport report = RaceDemo.run(config);
            // the race is statistical: if this run happened to be lucky, run again (up to a few times)
            for (int attempt = 1; attempt < RACE_ATTEMPTS && report.unsafeOversold() == 0; attempt++) {
                io.println("(the unsafe run was lucky this time - trying again)");
                report = RaceDemo.run(config);
            }
            for (String line : report.lines()) {
                io.println(line);
            }
            io.println(report.unsafeOversold() > 0 && report.safeOversold() == 0
                    ? "Same workload, different result: that is a race condition, and the lock cures it."
                    : "No oversell showed up this time; raise the pause or the thread count and run again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriveException("The demo was interrupted", e);
        }
    }

    private void lifecycle() {
        try {
            io.println("One worker thread, observed by the main thread:");
            for (Thread.State state : new ThreadLifecycleDemo().run()) {
                io.printf("  %-14s %s", state, ThreadLifecycleDemo.explain(state));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DriveException("The demo was interrupted", e);
        }
    }
}
