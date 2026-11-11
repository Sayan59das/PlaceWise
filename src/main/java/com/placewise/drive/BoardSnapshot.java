package com.placewise.drive;

import com.placewise.model.Application;
import com.placewise.model.Company;
import java.util.List;

/**
 * A consistent copy of one company's seat allocation, taken under its lock.
 *
 * @param company    the company
 * @param selected   applications holding a seat, best-ranked first
 * @param waitlisted applications waiting for a seat, best-ranked first
 */
public record BoardSnapshot(Company company, List<Application> selected, List<Application> waitlisted) {
}
