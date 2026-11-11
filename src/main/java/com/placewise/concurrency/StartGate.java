package com.placewise.concurrency;

/**
 * A one-shot starting gate: threads call {@link #awaitOpen()} and sleep until some thread calls
 * {@link #open()}, then they all run at the same moment. It is how the student threads start
 * "at the same time" and how the race demo makes sure every thread is really competing.
 */
public final class StartGate {

    private boolean open; // guarded by "this"

    /** Creates a closed gate. */
    public StartGate() {
        // starts closed
    }

    /** Opens the gate and wakes every waiting thread. Idempotent. */
    public synchronized void open() {
        open = true;
        notifyAll();
    }

    /**
     * Waits until the gate is open (returns immediately if it already is).
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized void awaitOpen() throws InterruptedException {
        while (!open) {
            wait();
        }
    }
}
