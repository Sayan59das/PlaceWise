package com.placewise.exception;

/** Thrown when a placement-drive simulation fails or is interrupted. */
public class DriveException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what went wrong
     */
    public DriveException(String message) {
        super(message);
    }

    /**
     * Creates the exception with the failure that caused it.
     *
     * @param message what went wrong
     * @param cause   the first uncaught throwable from a worker thread
     */
    public DriveException(String message, Throwable cause) {
        super(message, cause);
    }
}
