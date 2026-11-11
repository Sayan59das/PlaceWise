package com.placewise.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * What reading a CSV file produced.
 *
 * @param accepted good rows with their source position, in file order
 * @param rejected the rows that were refused, in file order
 * @param dataRows number of non-blank data rows seen (accepted + rejected)
 * @param <T>      the imported type
 */
public record ImportResult<T>(List<Row<T>> accepted, List<RejectedRow> rejected, int dataRows) {

    /**
     * An accepted row. The line number and original text are kept so a later step (for example "this
     * roll number already exists in the database") can still point at the exact line in the file.
     *
     * @param item       the object built from the row
     * @param lineNumber 1-based line number in the source file
     * @param rawLine    the line exactly as read
     * @param <T>        the imported type
     */
    public record Row<T>(T item, int lineNumber, String rawLine) {
    }

    /**
     * Convenience view of just the objects.
     *
     * @return the accepted objects in file order
     */
    public List<T> items() {
        List<T> items = new ArrayList<>();
        for (Row<T> row : accepted) {
            items.add(row.item());
        }
        return items;
    }
}
