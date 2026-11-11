package com.placewise.exception;

/** Thrown when a student is saved with a roll number that already exists. */
public class DuplicateRollNoException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param rollNo the roll number that is already taken
     */
    public DuplicateRollNoException(String rollNo) {
        super("Roll number already exists: " + rollNo);
    }

    /**
     * Creates the exception and keeps the database error that caused it.
     *
     * @param rollNo the roll number that is already taken
     * @param cause  the underlying {@code SQLException}
     */
    public DuplicateRollNoException(String rollNo, Throwable cause) {
        super("Roll number already exists: " + rollNo, cause);
    }
}
