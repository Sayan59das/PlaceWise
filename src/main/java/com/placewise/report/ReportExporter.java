package com.placewise.report;

import com.placewise.exception.PlaceWiseException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes rendered reports to disk with a character stream. */
public final class ReportExporter {

    private ReportExporter() {
    }

    /**
     * Renders a table and writes it to {@code directory/baseName + extension}.
     *
     * @param table     the report data
     * @param renderer  the file format
     * @param directory where to write; created if missing
     * @param baseName  file name without extension; characters other than letters, digits, dot, dash and
     *                  underscore are replaced so a company name can never escape the directory
     * @return the file that was written
     * @throws PlaceWiseException if the file cannot be written
     */
    public static Path export(Table table, ReportRenderer renderer, Path directory, String baseName) {
        Path file = directory.resolve(safeName(baseName) + renderer.extension());
        try {
            Files.createDirectories(directory);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8))) {
                writer.write(renderer.render(table));
            }
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot write the report " + file + ": " + e.getMessage(), e);
        }
        return file;
    }

    /**
     * Makes a text safe to use as a file name.
     *
     * @param name any text
     * @return the text with unsafe characters replaced by underscores; never empty
     */
    static String safeName(String name) {
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isEmpty() || cleaned.replace(".", "").isEmpty()) {
            return "report";
        }
        return cleaned;
    }
}
