package com.placewise.exception;

/**
 * Wraps a checked {@code SQLException}. Callers above the persistence layer then never need to import
 * {@code java.sql} or declare {@code throws SQLException}.
 */
public class PersistenceException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what the repository was trying to do
     * @param cause   the original {@code SQLException}
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception without a cause (for example "database is closed").
     *
     * @param message what went wrong
     */
    public PersistenceException(String message) {
        super(message);
    }
}
