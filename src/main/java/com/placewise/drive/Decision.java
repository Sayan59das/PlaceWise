package com.placewise.drive;

import com.placewise.audit.AuditEvent;
import com.placewise.model.Application;

/**
 * Something a {@link CompanyBoard} decided while holding its lock. The board only <em>returns</em> these;
 * the caller writes them to the audit log after the lock is released, so no thread ever holds two
 * locks at once (which is what rules out deadlock).
 *
 * @param event       the audit event type
 * @param application the application concerned
 * @param detail      why
 */
public record Decision(AuditEvent event, Application application, String detail) {

    /**
     * Text for the audit log.
     *
     * @return for example {@code "GEN0007 -> Acme Tech: seat available"}
     */
    public String message() {
        return application.student().rollNo() + " -> " + application.company().name() + ": " + detail;
    }
}
