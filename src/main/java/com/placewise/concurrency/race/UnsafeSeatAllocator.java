package com.placewise.concurrency.race;

/**
 * <b>Deliberately broken.</b> It checks "is a seat left?" and takes the seat in two separate steps with
 * no synchronization. Between the CHECK and the ACT another thread can check the same stale number, so
 * two (or more) threads each believe they got "the last seat": seats are oversold.
 *
 * <p>The {@code window} runnable stands for the slow work real code does between the two steps (a
 * database write, a network call). Widening it makes the race show up in a short demo; without it the
 * bug still exists, it just hides until production load.
 */
public final class UnsafeSeatAllocator implements SeatAllocator {

    private final int[] remaining; // shared and unprotected: that is the bug
    private final Runnable window;

    /**
     * Creates the allocator.
     *
     * @param seats  seats per company
     * @param window work done between the check and the update
     */
    public UnsafeSeatAllocator(int[] seats, Runnable window) {
        this.remaining = seats.clone();
        this.window = window;
    }

    @Override
    public boolean tryAllocate(int companyIndex) {
        if (remaining[companyIndex] > 0) {   // CHECK
            window.run();                    // other threads run here and pass the same check
            remaining[companyIndex]--;       // ACT: too late, and itself a non-atomic read-modify-write
            return true;
        }
        return false;
    }
}
