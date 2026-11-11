package com.placewise.audit;

/**
 * Where audit events go. An interface so the drive can write to a file in production
 * ({@link FileAuditLog}) and to nothing at all in a quick test ({@link #none()}).
 *
 * <p>Implementations must be safe to call from many threads at once.
 */
public interface AuditLog extends AutoCloseable {

    /**
     * Records one event, stamped with the time and the calling thread's name.
     *
     * @param event   what kind of event
     * @param message the details (line breaks are replaced by spaces)
     */
    void log(AuditEvent event, String message);

    /** Releases resources. The default does nothing; narrowing {@code throws Exception} away keeps callers simple. */
    @Override
    default void close() {
        // nothing to release
    }

    /**
     * An audit log that discards everything.
     *
     * @return a no-op log
     */
    static AuditLog none() {
        return (event, message) -> { };
    }
}
