package com.placewise.drive;

/** Thread-safe counters for the progress monitor. Plain {@code synchronized} methods guard both fields. */
public final class ProgressCounter {

    private long submitted; // guarded by "this"
    private long processed; // guarded by "this"

    /** Creates counters that start at zero. */
    public ProgressCounter() {
        // both counters start at zero
    }

    /** Records that a student put an application into the queue. */
    public synchronized void submitted() {
        submitted++;
    }

    /** Records that a recruiter finished handling an application. */
    public synchronized void processed() {
        processed++;
    }

    /**
     * Returns the number of submitted applications.
     *
     * @return the count so far
     */
    public synchronized long submittedCount() {
        return submitted;
    }

    /**
     * Returns the number of processed applications.
     *
     * @return the count so far
     */
    public synchronized long processedCount() {
        return processed;
    }
}
