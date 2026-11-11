package com.placewise.model;

/** Where an {@link Application} stands. */
public enum ApplicationStatus {
    /** Submitted, not yet looked at by a recruiter. */
    APPLIED,
    /** Holds a seat. */
    SELECTED,
    /** Ineligible, or a duplicate application. */
    REJECTED,
    /** Eligible but all seats are held by better-ranked students; promoted automatically on a withdrawal. */
    WAITLISTED,
    /** The student gave the seat (or waitlist place) back. Added to the four required statuses so a
     *  withdrawal is not mislabelled as a rejection. */
    WITHDRAWN
}
