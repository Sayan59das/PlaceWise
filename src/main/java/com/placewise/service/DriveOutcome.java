package com.placewise.service;

import com.placewise.drive.DriveResult;
import java.util.List;

/**
 * A finished drive plus the verdict of the invariant checker.
 *
 * @param result      statuses and allocations
 * @param violations  broken invariants; an empty list means the drive was correct
 * @param persistence one line saying whether the results were saved to storage
 */
public record DriveOutcome(DriveResult result, List<String> violations, String persistence) {
}
