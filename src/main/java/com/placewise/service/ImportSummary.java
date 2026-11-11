package com.placewise.service;

import java.nio.file.Path;

/**
 * Result of an import, ready to show to the user.
 *
 * @param dataRows    data rows found in the file
 * @param imported    rows stored
 * @param rejected    rows refused
 * @param rejectsFile the {@code rejects.csv} written, or {@code null} if nothing was refused
 */
public record ImportSummary(int dataRows, int imported, int rejected, Path rejectsFile) {

    /**
     * One-line description for the console.
     *
     * @return for example {@code "Imported 48 of 50 rows; 2 rejected (see rejects.csv)"}
     */
    public String describe() {
        String text = "Imported " + imported + " of " + dataRows + " rows; " + rejected + " rejected";
        return rejectsFile == null ? text : text + " (see " + rejectsFile + ")";
    }
}
