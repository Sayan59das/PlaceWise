package com.placewise.concurrency.race;

import com.placewise.exception.NoSeatsException;

/**
 * "Give me one seat at company number {@code n}, if any is left." Two implementations exist on purpose:
 * {@link SafeSeatAllocator} (correct) and {@link UnsafeSeatAllocator} (broken, for the race demo).
 */
public interface SeatAllocator {

    /**
     * Tries to take one seat.
     *
     * @param companyIndex which company, 0-based
     * @return {@code true} if a seat was granted
     */
    boolean tryAllocate(int companyIndex);

    /**
     * Takes a seat or fails loudly.
     *
     * @param companyIndex which company, 0-based
     * @param companyName  used in the error message
     * @throws NoSeatsException if no seat was left
     */
    default void allocate(int companyIndex, String companyName) {
        if (!tryAllocate(companyIndex)) {
            throw new NoSeatsException(companyName);
        }
    }
}
