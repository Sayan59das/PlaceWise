package com.placewise.exception;

/**
 * Thrown for problems with a whole import file (missing, empty, wrong header, unreadable).
 * Problems with a single <em>row</em> are never exceptions: they go to {@code rejects.csv}.
 */
public class ImportException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the file
     */
    public ImportException(String message) {
        super(message);
    }

    /**
     * Creates the exception with the I/O error that caused it.
     *
     * @param message what is wrong with the file
     * @param cause   the underlying {@code IOException}
     */
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
