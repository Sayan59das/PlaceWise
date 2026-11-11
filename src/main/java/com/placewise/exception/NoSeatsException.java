package com.placewise.exception;

/** Thrown when a seat is demanded from a company that has none left. */
public class NoSeatsException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param company the name of the full company
     */
    public NoSeatsException(String company) {
        super("No seats left at " + company);
    }
}
