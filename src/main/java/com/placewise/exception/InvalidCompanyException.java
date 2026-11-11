package com.placewise.exception;

import java.util.ArrayList;
import java.util.List;

/** Thrown when a {@code Company} breaks one or more validation rules. */
public class InvalidCompanyException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /** Declared as ArrayList because it is Serializable (exceptions are Serializable). */
    private final ArrayList<String> problems;

    /**
     * Creates the exception from the full list of violations.
     *
     * @param problems every validation message; must not be empty
     */
    public InvalidCompanyException(List<String> problems) {
        super("Invalid company: " + String.join("; ", problems));
        this.problems = new ArrayList<>(problems);
    }

    /**
     * Creates the exception for a single problem.
     *
     * @param problem the validation message
     */
    public InvalidCompanyException(String problem) {
        this(List.of(problem));
    }

    /**
     * Returns every violation that was found.
     *
     * @return an immutable list of messages
     */
    public List<String> problems() {
        return List.copyOf(problems);
    }
}
