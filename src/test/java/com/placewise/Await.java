package com.placewise;

import java.util.function.BooleanSupplier;

/**
 * Bounded polling for tests. A test must never "sleep and hope" that another thread got somewhere; it
 * polls an observable condition until it holds, and fails with a clear message after a deadline.
 */
public final class Await {

    private static final long TIMEOUT_NANOS = 10_000_000_000L;

    private Await() {
    }

    /** Waits until the thread reaches the given state (for example WAITING inside Object.wait). */
    public static void state(Thread thread, Thread.State wanted) {
        until(() -> thread.getState() == wanted, "thread " + thread.getName() + " to be " + wanted
                + " (was " + thread.getState() + ")");
    }

    /** Waits until the condition holds. */
    public static void until(BooleanSupplier condition, String description) {
        long deadline = System.nanoTime() + TIMEOUT_NANOS;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for " + description);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for " + description, e);
            }
        }
    }

    /** Waits for a thread to finish. */
    public static void finished(Thread thread) {
        try {
            thread.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while joining " + thread.getName(), e);
        }
        if (thread.isAlive()) {
            throw new AssertionError("Thread " + thread.getName() + " did not finish; state " + thread.getState());
        }
    }
}
