package com.placewise.csv;

import com.placewise.exception.PlaceWiseException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Writes CSV files through character streams ({@link FileWriter} wrapped in a {@link BufferedWriter}). */
public final class CsvFiles {

    private CsvFiles() {
    }

    /**
     * Writes a header and rows, replacing any existing file.
     *
     * @param file   target file; missing parent directories are created
     * @param header column names
     * @param rows   one list of values per line
     * @throws PlaceWiseException if the file cannot be written
     */
    public static void write(Path file, List<String> header, List<List<String>> rows) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8))) {
                writer.write(CsvLine.format(header));
                writer.newLine();
                for (List<String> row : rows) {
                    writer.write(CsvLine.format(row));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot write " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Writes the rows that an import refused, so the user can fix and re-import them.
     *
     * @param file     the {@code rejects.csv} to create
     * @param rejected the refused rows
     */
    public static void writeRejects(Path file, List<RejectedRow> rejected) {
        List<List<String>> rows = new ArrayList<>();
        for (RejectedRow row : rejected) {
            rows.add(List.of(String.valueOf(row.lineNumber()), row.reason(), row.rawLine()));
        }
        write(file, List.of("line", "reason", "row"), rows);
    }
}
