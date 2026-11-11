package com.placewise.ui;

/**
 * The input ended (Ctrl+D / Ctrl+Z, or a scripted test ran out of lines). It unwinds to the main loop,
 * which then shuts the program down gracefully instead of waiting for input that will never come.
 */
public class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates the exception. */
    public InputClosedException() {
        super("Input ended");
    }
}
