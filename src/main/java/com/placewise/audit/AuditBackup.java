package com.placewise.audit;

import com.placewise.exception.PlaceWiseException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copies a file <em>byte by byte</em> with {@link FileInputStream}/{@link FileOutputStream}. A byte
 * stream (not a character stream) is the right tool for a backup: it must be an exact copy no matter
 * what encoding the text uses.
 */
public final class AuditBackup {

    private static final int BUFFER_SIZE = 8192;

    private AuditBackup() {
    }

    /**
     * Copies {@code source} to {@code target}, replacing the target if it exists.
     *
     * @param source the file to copy
     * @param target the copy to create
     * @return the number of bytes copied
     * @throws PlaceWiseException if the source is missing or the copy fails
     */
    public static long copy(Path source, Path target) {
        if (!Files.isRegularFile(source)) {
            throw new PlaceWiseException("Nothing to back up: " + source + " does not exist");
        }
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream in = new FileInputStream(source.toFile());
                 OutputStream out = new FileOutputStream(target.toFile())) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                }
                return total;
            }
        } catch (IOException e) {
            throw new PlaceWiseException("Backup of " + source + " failed: " + e.getMessage(), e);
        }
    }
}
