package com.placewise.ranking;

import com.placewise.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks students by academic performance.
 *
 * <p>Order: higher CGPA first; if equal, fewer active backlogs; then name (ignoring case); then roll
 * number. Roll numbers are unique, so two students never tie and the result never depends on the input
 * order.
 */
public final class StudentRanking {

    /** The ranking order, used everywhere a "best first" list is needed. */
    public static final Comparator<Student> ORDER = new RankingOrder();

    private StudentRanking() {
    }

    /** Ranks all students, best first, numbered from 1. */
    public static List<RankedStudent> rank(Collection<Student> students) {
        List<Student> sorted = new ArrayList<>(students);
        sorted.sort(ORDER);
        List<RankedStudent> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ranked.add(new RankedStudent(i + 1, sorted.get(i)));
        }
        return ranked;
    }

    /** The best {@code n} students (0 gives an empty list; more than the size gives everyone). */
    public static List<RankedStudent> top(Collection<Student> students, int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative: " + n);
        }
        return rank(students).stream().limit(n).toList();
    }

    /** One page of the ranking; {@code page} starts at 1. A page past the end is empty. */
    public static List<RankedStudent> page(Collection<Student> students, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize must be at least 1");
        }
        long skipped = (long) (page - 1) * pageSize;
        // A stream reads like a sentence here: skip the earlier pages, then take one page.
        return rank(students).stream().skip(skipped).limit(pageSize).toList();
    }

    /** The ranking order written out as a plain Comparator class: one step per tie-breaker. */
    private static final class RankingOrder implements Comparator<Student> {

        @Override
        public int compare(Student a, Student b) {
            int byCgpa = b.cgpa().compareTo(a.cgpa()); // b before a: higher CGPA comes first
            if (byCgpa != 0) {
                return byCgpa;
            }
            int byBacklogs = Integer.compare(a.activeBacklogs(), b.activeBacklogs()); // fewer first
            if (byBacklogs != 0) {
                return byBacklogs;
            }
            int byName = a.name().compareToIgnoreCase(b.name());
            if (byName != 0) {
                return byName;
            }
            return a.rollNo().compareTo(b.rollNo());
        }
    }
}
