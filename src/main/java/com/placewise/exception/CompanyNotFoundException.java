package com.placewise.exception;

/** Thrown when a company id (or name) does not exist. */
public class CompanyNotFoundException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception for a missing id.
     *
     * @param id the company id that was looked up
     */
    public CompanyNotFoundException(long id) {
        super("No company with id " + id);
    }

    /**
     * Creates the exception for a missing name.
     *
     * @param name the company name that was looked up
     */
    public CompanyNotFoundException(String name) {
        super("No company named '" + name + "'");
    }
}
