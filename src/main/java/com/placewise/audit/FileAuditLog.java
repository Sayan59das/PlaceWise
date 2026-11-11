package com.placewise.audit;

import com.placewise.exception.PlaceWiseException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Thread-safe, append-only audit log in a plain text file. One line per event:
 * {@code timestamp | thread | EVENT | message}.
 *
 * <p>{@link #log} is {@code synchronized}: a line is written and flushed while holding the lock, so two
 * threads can never interleave characters inside one line (this is exactly the race the 8-thread test
 * tries to provoke). Opening in {@code APPEND} mode means earlier runs are never overwritten.
 */
public final class FileAuditLog implements AuditLog {

    private final Path file;
    private final BufferedWriter writer;
    private boolean closed; // guarded by "this"

    /**
     * Opens (or creates) the log file.
     *
     * @param file the log file; missing parent directories are created
     * @throws PlaceWiseException if the file cannot be opened
     */
    public FileAuditLog(Path file) {
        this.file = file;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot open the audit log " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void log(AuditEvent event, String message) {
        if (closed) {
            throw new IllegalStateException("The audit log is closed");
        }
        String line = Instant.now() + " | " + Thread.currentThread().getName() + " | " + event.name()
                + " | " + message.replace('\r', ' ').replace('\n', ' ');
        try {
            writer.write(line);
            writer.newLine();
            writer.flush(); // flush per line so a crash (or the backup copy) never misses finished events
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot write to the audit log: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the log file.
     *
     * @return the path given to the constructor
     */
    public Path file() {
        return file;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            writer.close();
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot close the audit log: " + e.getMessage(), e);
        }
    }
}
