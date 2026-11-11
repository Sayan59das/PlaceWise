package com.placewise.model;

/**
 * Identity of an application: one student can apply to one company once.
 *
 * @param rollNo    the student's roll number (normalized)
 * @param companyId the company id
 */
public record ApplicationKey(String rollNo, long companyId) {
}
