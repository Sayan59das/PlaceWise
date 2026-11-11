package com.placewise.concurrency;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks one thread through every {@link Thread.State} and records what the main thread sees:
 * <pre>
 *   NEW            - created, not started
 *   RUNNABLE       - running (here: spinning on a volatile flag)
 *   BLOCKED        - wants a monitor that another thread (main) holds
 *   WAITING        - inside Object.wait(), until notify
 *   TIMED_WAITING  - inside Thread.sleep(ms)
 *   TERMINATED     - run() has finished (main waited for it with join())
 * </pre>
 * Nothing here relies on luck: after each step the main thread polls the worker's state until it reaches
 * the expected one (with a deadline), so the demo is deterministic.
 */
public final class ThreadLifecycleDemo {

    private static final long SLEEP_MILLIS = 300;
    private static final long TIMEOUT_NANOS = 10_000_000_000L;

    private final Object lock = new Object();
    private volatile boolean spinning = true; // volatile: the worker must see main's write promptly
    private boolean released;                 // guarded by "lock"

    /** Creates a demo; each instance can be {@link #run()} once. */
    public ThreadLifecycleDemo() {
        // fields start in their "worker spins, not released" state
    }

    /**
     * Runs the demo.
     *
     * @return the states in the order they were observed
     * @throws InterruptedException if the calling thread is interrupted
     * @throws IllegalStateException if the worker never reaches an expected state within 10 seconds
     */
    public List<Thread.State> run() throws InterruptedException {
        List<Thread.State> seen = new ArrayList<>();
        Thread worker = new Thread(this::work, "lifecycle-worker");
        seen.add(worker.getState());                       // NEW
        synchronized (lock) {                              // main holds the monitor on purpose
            worker.start();
            awaitState(worker, Thread.State.RUNNABLE, seen);
            spinning = false;                              // worker now tries to enter synchronized(lock)...
            awaitState(worker, Thread.State.BLOCKED, seen); // ...and cannot, because main still holds it
        }                                                  // main lets go; worker enters and calls wait()
        awaitState(worker, Thread.State.WAITING, seen);
        synchronized (lock) {
            released = true;
            lock.notifyAll();                              // worker wakes, leaves wait(), goes to sleep
        }
        awaitState(worker, Thread.State.TIMED_WAITING, seen);
        worker.join();                                     // main itself WAITS here until the worker dies
        seen.add(worker.getState());                       // TERMINATED
        return seen;
    }

    private void work() {
        while (spinning) {
            Thread.onSpinWait();
        }
        synchronized (lock) {
            while (!released) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitState(Thread worker, Thread.State wanted, List<Thread.State> seen)
            throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT_NANOS;
        while (worker.getState() != wanted) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("The worker never reached " + wanted);
            }
            Thread.sleep(1); // bounded polling of a state we cannot be notified about
        }
        seen.add(wanted);
    }

    /**
     * Explains a state in one line, for the console.
     *
     * @param state a thread state
     * @return a short explanation
     */
    public static String explain(Thread.State state) {
        return switch (state) {
            case NEW -> "created with new Thread(...), start() not called yet";
            case RUNNABLE -> "running (or ready to run) - here it spins on a volatile flag";
            case BLOCKED -> "waiting to enter a synchronized block whose lock another thread holds";
            case WAITING -> "inside Object.wait(): sleeps until another thread calls notify";
            case TIMED_WAITING -> "inside Thread.sleep(ms): sleeps for a fixed time";
            case TERMINATED -> "run() finished; main saw this after join() returned";
        };
    }
}
