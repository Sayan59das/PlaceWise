package com.placewise.model;

import java.util.Objects;

/**
 * A student's application to a company. Unlike {@link Student} and {@link Company} it is
 * <em>mutable</em>: its status changes while a placement drive runs.
 *
 * <p>The status field is {@code volatile} so that a thread reading it (for example the progress monitor
 * or the main thread after {@code join}) always sees the latest value written by a recruiter. Writes are
 * only made while holding the owning company's lock, which keeps "read-then-change" decisions atomic.
 */
public final class Application {

    private final Student student;
    private final Company company;
    private volatile ApplicationStatus status;

    /**
     * Creates a fresh application in the {@link ApplicationStatus#APPLIED} state.
     *
     * @param student the applicant
     * @param company the company applied to
     */
    public Application(Student student, Company company) {
        this(student, company, ApplicationStatus.APPLIED);
    }

    /**
     * Creates an application in a given state (used when loading from the database).
     *
     * @param student the applicant
     * @param company the company applied to
     * @param status  the current status
     */
    public Application(Student student, Company company, ApplicationStatus status) {
        this.student = Objects.requireNonNull(student, "student");
        this.company = Objects.requireNonNull(company, "company");
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * Returns the applicant.
     *
     * @return the student
     */
    public Student student() {
        return student;
    }

    /**
     * Returns the target company.
     *
     * @return the company
     */
    public Company company() {
        return company;
    }

    /**
     * Returns the current status.
     *
     * @return the latest status written by any thread
     */
    public ApplicationStatus status() {
        return status;
    }

    /**
     * Changes the status. Callers inside a drive must hold the company's lock.
     *
     * @param newStatus the new status
     */
    public void setStatus(ApplicationStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "status");
    }

    /**
     * Returns the database identity of this application.
     *
     * @return roll number and company id
     */
    public ApplicationKey key() {
        return new ApplicationKey(student.rollNo(), company.id());
    }

    @Override
    public String toString() {
        return student.rollNo() + " -> " + company.name() + " [" + status + "]";
    }
}
