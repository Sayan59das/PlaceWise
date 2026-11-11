package com.placewise.exception;

/**
 * Root of every error PlaceWise raises on purpose.
 *
 * <p>It is <em>unchecked</em> (extends {@link RuntimeException}) so that business-rule failures do not
 * force {@code throws} clauses through every layer; the console menu catches this one type at the top
 * level and prints the message, so a bad input can never crash the program.
 */
public class PlaceWiseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a human-readable message.
     *
     * @param message what went wrong, phrased for the end user
     */
    public PlaceWiseException(String message) {
        super(message);
    }

    /**
     * Creates an exception that wraps a lower-level cause (for example an {@code SQLException}).
     *
     * @param message what went wrong, phrased for the end user
     * @param cause   the original failure, kept so stack traces stay useful
     */
    public PlaceWiseException(String message, Throwable cause) {
        super(message, cause);
    }
}
