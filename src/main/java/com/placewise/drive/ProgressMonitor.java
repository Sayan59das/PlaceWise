package com.placewise.drive;

import com.placewise.concurrency.BoundedQueue;
import java.io.PrintStream;

/**
 * Prints a progress line every so often while a drive runs. It runs on a <em>daemon</em> thread: if the
 * program ends, the JVM does not wait for it. It spends its life in {@code Thread.sleep}
 * (TIMED_WAITING) and stops when {@link #stop()} clears the {@code volatile} flag and interrupts the sleep.
 */
public final class ProgressMonitor implements Runnable {

    private final PrintStream out;
    private final ProgressCounter counter;
    private final BoundedQueue<?> queue;
    private final int totalApplications;
    private final long intervalMillis;
    private volatile boolean running = true; // written by main, read by the monitor thread
    private Thread thread;

    ProgressMonitor(PrintStream out, ProgressCounter counter, BoundedQueue<?> queue, int totalApplications,
                    long intervalMillis) {
        this.out = out;
        this.counter = counter;
        this.queue = queue;
        this.totalApplications = totalApplications;
        this.intervalMillis = intervalMillis;
    }

    /**
     * Starts the daemon thread.
     *
     * @return the started thread
     */
    Thread start() {
        thread = new Thread(this, "progress-monitor");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Stops the monitor and waits for its thread to finish.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    void stop() throws InterruptedException {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                return; // stop() interrupted the sleep: leave quietly
            }
            if (running) {
                out.println("  [progress] submitted " + counter.submittedCount() + ", processed "
                        + counter.processedCount() + " of " + totalApplications + " applications, queue "
                        + queue.size() + "/" + queue.capacity());
            }
        }
    }
}
