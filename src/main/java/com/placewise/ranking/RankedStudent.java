package com.placewise.ranking;

import com.placewise.model.Student;

/**
 * A student with a position in a ranked list. Positions are distinct (1, 2, 3, ...) because the
 * tie-break rules in {@link StudentRanking} always produce a strict order.
 *
 * @param rank    1-based position in the <em>full</em> ranking, also on later pages
 * @param student the student
 */
public record RankedStudent(int rank, Student student) {
}
