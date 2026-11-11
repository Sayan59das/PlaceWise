package com.placewise.exception;

/** Thrown when a roll number does not exist. */
public class StudentNotFoundException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param rollNo the roll number that was looked up
     */
    public StudentNotFoundException(String rollNo) {
        super("No student with roll number " + rollNo);
    }
}
