package com.placewise.exception;

import java.util.ArrayList;
import java.util.List;

/** Thrown when a {@code Student} breaks one or more validation rules. */
public class InvalidStudentException extends PlaceWiseException {

    private static final long serialVersionUID = 1L;

    /** Every rule violation (never just the first one). Declared as ArrayList because it is Serializable. */
    private final ArrayList<String> problems;

    /**
     * Creates the exception from the full list of violations.
     *
     * @param problems every validation message; must not be empty
     */
    public InvalidStudentException(List<String> problems) {
        super("Invalid student: " + String.join("; ", problems));
        this.problems = new ArrayList<>(problems);
    }

    /**
     * Creates the exception for a single problem.
     *
     * @param problem the validation message
     */
    public InvalidStudentException(String problem) {
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
