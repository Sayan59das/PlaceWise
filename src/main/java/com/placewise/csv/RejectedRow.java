package com.placewise.csv;

/**
 * A data row that could not be imported.
 *
 * @param lineNumber 1-based line number in the source file (the header is line 1)
 * @param rawLine    the line exactly as read
 * @param reason     why it was refused
 */
public record RejectedRow(int lineNumber, String rawLine, String reason) {
}
