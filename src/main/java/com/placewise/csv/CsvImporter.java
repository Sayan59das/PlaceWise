package com.placewise.csv;

import com.placewise.exception.ImportException;
import com.placewise.exception.PlaceWiseException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a CSV file into objects of type {@code T}. It is a <em>generic abstract class</em>: it does the
 * reading, and a child class ({@link StudentCsv}, {@link CompanyCsv}) says how one row becomes an object.
 *
 * <p>Two kinds of problem are kept apart on purpose:
 * <ul>
 *   <li>a problem with the whole <em>file</em> (missing, empty, wrong header) throws {@link ImportException};</li>
 *   <li>a problem with one <em>row</em> never stops the import: the row is put in the rejected list with
 *       its line number and reason, and reading continues with the next row.</li>
 * </ul>
 */
public abstract class CsvImporter<T> {

    private final List<String> expectedHeader;

    /** Stores the column names the first line of the file must have. */
    protected CsvImporter(List<String> expectedHeader) {
        this.expectedHeader = List.copyOf(expectedHeader);
    }

    /** Turns the fields of one row into an object; throws an exception with a readable reason if the row is bad. */
    protected abstract T parseRow(List<String> fields);

    /** The unique key of an object (for example the roll number). A repeated key rejects the later row. */
    protected abstract String keyOf(T item);

    /** Reads the whole file and returns the accepted objects and the rejected rows. */
    public ImportResult<T> read(Path file) {
        if (!Files.exists(file)) {
            throw new ImportException("File not found: " + file);
        }
        if (Files.isDirectory(file)) {
            throw new ImportException("That is a folder, not a CSV file: " + file);
        }
        // try-with-resources: the reader is closed automatically, even if an exception is thrown.
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ImportException("The file is empty (no header line): " + file);
            }
            checkHeader(headerLine);
            return readRows(reader);
        } catch (IOException e) {
            throw new ImportException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    private void checkHeader(String headerLine) {
        // Excel sometimes puts an invisible "byte order mark" in front of the first column name.
        String line = headerLine.startsWith("﻿") ? headerLine.substring(1) : headerLine;
        List<String> actual;
        try {
            actual = CsvLine.parse(line);
        } catch (IllegalArgumentException e) {
            throw new ImportException("The header line is not valid CSV: " + e.getMessage());
        }
        boolean same = actual.size() == expectedHeader.size();
        for (int i = 0; same && i < actual.size(); i++) {
            same = actual.get(i).trim().toLowerCase(Locale.ROOT).equals(expectedHeader.get(i).toLowerCase(Locale.ROOT));
        }
        if (!same) {
            throw new ImportException("Unexpected header. Expected: " + String.join(",", expectedHeader)
                    + " but found: " + headerLine.trim());
        }
    }

    private ImportResult<T> readRows(BufferedReader reader) throws IOException {
        List<ImportResult.Row<T>> accepted = new ArrayList<>();
        List<RejectedRow> rejected = new ArrayList<>();
        Map<String, Integer> firstLineOfKey = new HashMap<>(); // key -> line where we first saw it
        int lineNumber = 1;                                   // line 1 was the header
        int dataRows = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            dataRows++;
            try {
                T item = parseRow(CsvLine.parse(line));
                String key = keyOf(item);
                if (firstLineOfKey.containsKey(key)) {
                    rejected.add(new RejectedRow(lineNumber, line,
                            "duplicate of '" + key + "' already seen on line " + firstLineOfKey.get(key)));
                } else {
                    firstLineOfKey.put(key, lineNumber);
                    accepted.add(new ImportResult.Row<>(item, lineNumber, line));
                }
            } catch (IllegalArgumentException | ArithmeticException | PlaceWiseException e) {
                // Multi-catch: each of these means "this row is bad", and none of them may stop the import.
                rejected.add(new RejectedRow(lineNumber, line, String.valueOf(e.getMessage())));
            }
        }
        return new ImportResult<>(List.copyOf(accepted), List.copyOf(rejected), dataRows);
    }
}
