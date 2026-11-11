package com.placewise.drive;

import com.placewise.audit.AuditLog;
import com.placewise.concurrency.BoundedQueue;
import com.placewise.concurrency.StartGate;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.exception.DriveException;
import com.placewise.model.Application;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs one placement drive with real threads.
 *
 * <pre>
 *  student threads --put--> [ BoundedQueue ] --take--> recruiter threads --admit--> CompanyBoard (1 lock each)
 *                                                                  after all done: withdrawal threads --withdraw--> boards
 * </pre>
 * Phase 1: every student thread waits at a {@link StartGate}, then they all submit at once. Recruiters
 * check eligibility and allocate seats. Phase 2: some selected students withdraw (concurrently) and the
 * waitlist is promoted. The result does not depend on thread timing: the plan is seeded, and the board
 * logic makes the final allocation a function of the set of applications only.
 */
public final class PlacementDrive {

    /** Student threads are capped; with more students each thread submits for several of them. */
    static final int MAX_STUDENT_THREADS = 256;
    private static final long MONITOR_INTERVAL_MILLIS = 200;

    private final List<Student> students;
    private final List<Company> companies;
    private final DriveConfig config;
    private final EligibilityChecker checker;
    private final AuditLog audit;
    private final PrintStream progressOut;

    /**
     * Creates a drive.
     *
     * @param students    the applicants
     * @param companies   the companies (their ids must be distinct)
     * @param config      thread counts, seed, probabilities
     * @param checker     the eligibility rules
     * @param audit       where events are logged
     * @param progressOut where the daemon progress monitor prints, or {@code null} for silence
     */
    public PlacementDrive(List<Student> students, List<Company> companies, DriveConfig config,
                          EligibilityChecker checker, AuditLog audit, PrintStream progressOut) {
        Set<Long> ids = new HashSet<>();
        for (Company company : companies) {
            if (!ids.add(company.id())) {   // add() returns false when the id was already in the set
                throw new IllegalArgumentException("Company ids must be distinct");
            }
        }
        this.students = List.copyOf(students);
        this.companies = List.copyOf(companies);
        this.config = config;
        this.checker = checker;
        this.audit = audit;
        this.progressOut = progressOut;
    }

    /**
     * Runs the whole drive and waits for every thread to finish.
     *
     * @return the final statuses and allocations
     * @throws DriveException if a worker thread failed or the calling thread was interrupted
     */
    public DriveResult run() {
        long startNanos = System.nanoTime();
        Map<Long, CompanyBoard> boards = new LinkedHashMap<>();
        for (Company company : companies) {
            boards.put(company.id(), new CompanyBoard(company));
        }
        List<List<Application>> plans = buildPlans();
        List<Application> all = new ArrayList<>();
        for (List<Application> plan : plans) {
            all.addAll(plan);
        }

        BoundedQueue<Application> queue = new BoundedQueue<>(config.queueCapacity());
        ProgressCounter progress = new ProgressCounter();
        StartGate gate = new StartGate();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        ProgressMonitor monitor = progressOut == null ? null
                : new ProgressMonitor(progressOut, progress, queue, all.size(), MONITOR_INTERVAL_MILLIS);
        boolean interrupted = false;
        try {
            List<Thread> recruiters = new ArrayList<>();
            for (int i = 1; i <= config.recruiters(); i++) {
                recruiters.add(newThread(new Recruiter(queue, boards, checker, audit, progress),
                        "recruiter-" + i, failures, queue));
            }
            List<Thread> applicants = new ArrayList<>();
            for (int i = 0; i < plans.size(); i++) {
                applicants.add(newThread(new StudentApplicant(plans.get(i), queue, audit, progress, gate),
                        "student-" + (i + 1), failures, queue));
            }
            threads.addAll(recruiters);
            threads.addAll(applicants);
            startAll(recruiters);
            startAll(applicants);
            if (monitor != null) {
                monitor.start();
            }
            gate.open();                       // "go!": all student threads submit at the same moment
            joinAll(applicants);               // join(): wait until every producer has finished
            queue.close();                     // producers are done: tell consumers to drain and stop
            joinAll(recruiters);
            throwIfFailed(failures);
            runWithdrawals(boards, failures, queue);
            throwIfFailed(failures);
        } catch (InterruptedException e) {
            // Do NOT call interrupt() here: with the flag set, the join() calls in shutDown() would throw
            // at once and leave workers running. The flag is restored after the shutdown, in finally.
            interrupted = true;
            throw new DriveException("The drive was interrupted", e);
        } finally {
            shutDown(queue, monitor, threads);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        List<BoardSnapshot> snapshots = new ArrayList<>();
        for (CompanyBoard board : boards.values()) {
            snapshots.add(board.snapshot());
        }
        return new DriveResult(config, companies, all, snapshots, Duration.ofNanos(System.nanoTime() - startNanos));
    }

    /** Decides, with a seeded random generator, which applications each student thread will submit. */
    private List<List<Application>> buildPlans() {
        List<List<Application>> plans = new ArrayList<>();
        if (students.isEmpty() || companies.isEmpty()) {
            return plans;
        }
        int threadCount = Math.min(students.size(), MAX_STUDENT_THREADS);
        for (int i = 0; i < threadCount; i++) {
            plans.add(new ArrayList<>());
        }
        int maxPerStudent = Math.min(config.maxApplicationsPerStudent(), companies.size());
        for (int i = 0; i < students.size(); i++) {
            Random random = new Random(config.seed() * 1_000_003L + i);
            List<Company> shuffled = new ArrayList<>(companies);
            Collections.shuffle(shuffled, random);
            List<Application> mine = plans.get(i % threadCount);
            int count = 1 + random.nextInt(maxPerStudent);
            for (int c = 0; c < count; c++) {
                mine.add(new Application(students.get(i), shuffled.get(c)));
            }
            if (random.nextInt(100) < config.duplicatePercent()) {
                mine.add(new Application(students.get(i), shuffled.get(0))); // deliberate duplicate
            }
        }
        return plans;
    }

    private void runWithdrawals(Map<Long, CompanyBoard> boards, List<Throwable> failures,
                                BoundedQueue<Application> queue) throws InterruptedException {
        Random random = new Random(config.seed() ^ 0x9E3779B97F4A7C15L);
        List<Application> chosen = new ArrayList<>();
        for (CompanyBoard board : boards.values()) {
            for (Application application : board.snapshot().selected()) { // best first: deterministic order
                if (random.nextInt(100) < config.withdrawalPercent()) {
                    chosen.add(application);
                }
            }
        }
        int workerCount = Math.min(config.recruiters(), chosen.size());
        List<List<Application>> slices = new ArrayList<>();
        for (int w = 0; w < workerCount; w++) {
            slices.add(new ArrayList<>());
        }
        for (int i = 0; i < chosen.size(); i++) {
            slices.get(i % workerCount).add(chosen.get(i));
        }
        List<Thread> workers = new ArrayList<>();
        for (int w = 0; w < workerCount; w++) {
            workers.add(newThread(new WithdrawalWorker(slices.get(w), boards, audit), "withdraw-" + (w + 1),
                    failures, queue));
        }
        startAll(workers);
        joinAll(workers);
    }

    /** Creates (does not start) a thread whose uncaught exception is recorded and unblocks everyone. */
    private static Thread newThread(Runnable task, String name, List<Throwable> failures,
                                    BoundedQueue<Application> queue) {
        Thread thread = new Thread(task, name);
        thread.setUncaughtExceptionHandler((t, error) -> {
            failures.add(new IllegalStateException("Thread " + t.getName() + " failed", error));
            queue.close(); // wake any thread blocked on the queue so the whole drive can unwind
        });
        return thread;
    }

    private static void startAll(List<Thread> threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    /** join() makes the calling thread wait until the other thread has finished. */
    private static void joinAll(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void throwIfFailed(List<Throwable> failures) {
        synchronized (failures) {
            if (!failures.isEmpty()) {
                throw new DriveException(failures.get(0).getMessage(), failures.get(0));
            }
        }
    }

    /** Always runs: closes the queue, stops the monitor, and makes sure no worker outlives the drive. */
    private static void shutDown(BoundedQueue<Application> queue, ProgressMonitor monitor, List<Thread> threads) {
        queue.close();
        boolean interrupted = false;
        try {
            if (monitor != null) {
                monitor.stop();
            }
            for (Thread thread : threads) {
                if (thread.isAlive()) { // only on the failure path: normal runs have joined everyone
                    thread.interrupt();
                    thread.join();
                }
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
