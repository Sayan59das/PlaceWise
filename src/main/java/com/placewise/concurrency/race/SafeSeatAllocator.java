package com.placewise.concurrency.race;

/**
 * The correct version: the check and the update happen inside one {@code synchronized} block on a lock
 * that belongs to that company, so they are <em>atomic</em> with respect to every other thread asking
 * for the same company. Different companies have different locks and never wait for each other.
 *
 * <p>No method takes a second lock while holding one, so the lock graph has no cycle and deadlock is
 * impossible.
 */
public final class SafeSeatAllocator implements SeatAllocator {

    private final int[] remaining; // element i is only touched while holding locks[i]
    private final Object[] locks;
    private final Runnable window;

    /**
     * Creates the allocator.
     *
     * @param seats  seats per company
     * @param window the same slow work the unsafe version does; here it runs inside the lock
     */
    public SafeSeatAllocator(int[] seats, Runnable window) {
        this.remaining = seats.clone();
        this.locks = new Object[seats.length];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        this.window = window;
    }

    @Override
    public boolean tryAllocate(int companyIndex) {
        synchronized (locks[companyIndex]) {
            if (remaining[companyIndex] > 0) { // CHECK ...
                window.run();
                remaining[companyIndex]--;     // ... and ACT, with nobody able to slip in between
                return true;
            }
            return false;
        }
    }

    /**
     * Seats still free at a company.
     *
     * @param companyIndex which company, 0-based
     * @return the free seats (never negative)
     */
    public int seatsLeft(int companyIndex) {
        synchronized (locks[companyIndex]) {
            return remaining[companyIndex];
        }
    }
}
