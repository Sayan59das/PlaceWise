package com.placewise.audit;

/** The kinds of line the audit log can hold. */
public enum AuditEvent {
    /** Someone asked "is this student eligible for this company?". */
    ELIGIBILITY_CHECK,
    /** A student submitted an application. */
    APPLICATION,
    /** A student got a seat (including a promotion from the waitlist). */
    SELECTED,
    /** A student is eligible but has no seat (yet). */
    WAITLISTED,
    /** An application was refused (ineligible or duplicate). */
    REJECTED,
    /** A student gave a seat or waitlist place back. */
    WITHDRAWN
}
