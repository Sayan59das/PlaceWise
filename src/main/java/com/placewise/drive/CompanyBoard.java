package com.placewise.drive;

import com.placewise.audit.AuditEvent;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.ranking.StudentRanking;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The seat allocation of ONE company. It owns <em>one private lock</em>; every method that reads or
 * changes the allocation does so inside {@code synchronized (lock)}, so "check the seats, then take one"
 * can never be interleaved with another thread doing the same thing (the race that
 * {@link UnsafeSeatAllocator} demonstrates).
 *
 * <p>Selection follows the ranking, whatever the arrival order. Two priority queues hold the state:
 * <ul>
 *   <li>{@code selected}: at most {@code seats} applications, the <em>worst</em> ranked on top;</li>
 *   <li>{@code waitlist}: everyone else who is eligible, the <em>best</em> ranked on top.</li>
 * </ul>
 * A new applicant takes a free seat, or displaces the worst selected applicant if the newcomer ranks
 * better (the displaced one drops to the waitlist), or joins the waitlist. When a selected student
 * withdraws, the best waitlisted applicant is promoted. After all applications are in, the selected set
 * is therefore exactly "the top {@code seats} eligible applicants by rank".
 *
 * <p>Deadlock-freedom: a thread never holds more than one board lock, and never calls out to another
 * lock (the audit log, queue, ...) while holding it; {@link Decision}s are returned and logged later.
 */
public final class CompanyBoard {

    /** Orders applications by their student's ranking: the best-ranked student comes first. */
    private static final Comparator<Application> BEST_FIRST =
            (a, b) -> StudentRanking.ORDER.compare(a.student(), b.student());

    private final Company company;
    private final Object lock = new Object(); // the one lock of this company
    private final PriorityQueue<Application> selected;
    private final PriorityQueue<Application> waitlist = new PriorityQueue<>(BEST_FIRST);
    private final Set<String> seenStudents = new HashSet<>(); // duplicate detection, guarded by lock

    /**
     * Creates an empty board.
     *
     * @param company the company whose seats are managed
     */
    public CompanyBoard(Company company) {
        this.company = company;
        // reversed(): the WORST selected application sits on top, so it is the one we can replace quickly.
        this.selected = new PriorityQueue<>(Math.max(1, company.seats()), BEST_FIRST.reversed());
    }

    /**
     * Returns the company.
     *
     * @return the company (immutable)
     */
    public Company company() {
        return company;
    }

    /**
     * Handles an eligible application.
     *
     * @param application the application, which must be for this company
     * @return what was decided (selection, displacement, waitlisting or rejection as a duplicate)
     */
    public List<Decision> admit(Application application) {
        if (application.company().id() != company.id()) {
            throw new IllegalArgumentException("Application is for another company: " + application);
        }
        synchronized (lock) {
            List<Decision> decisions = new ArrayList<>(3);
            String roll = application.student().rollNo();
            if (!seenStudents.add(roll)) {
                application.setStatus(ApplicationStatus.REJECTED);
                decisions.add(new Decision(AuditEvent.REJECTED, application, "duplicate application"));
                return decisions;
            }
            Application worst = selected.peek();
            if (selected.size() < company.seats()) {
                select(application, "seat available", decisions);
            } else if (worst != null && BEST_FIRST.compare(application, worst) < 0) {
                selected.poll();
                worst.setStatus(ApplicationStatus.WAITLISTED);
                waitlist.add(worst);
                decisions.add(new Decision(AuditEvent.WAITLISTED, worst, "displaced by better-ranked " + roll));
                select(application, "outranks " + worst.student().rollNo(), decisions);
            } else {
                application.setStatus(ApplicationStatus.WAITLISTED);
                waitlist.add(application);
                decisions.add(new Decision(AuditEvent.WAITLISTED, application,
                        "all seats held by better-ranked students"));
            }
            return decisions;
        }
    }

    /**
     * A student gives back a seat or a waitlist place. If a seat was freed, the best waitlisted
     * applicant is promoted in the same atomic step.
     *
     * @param application the application that withdraws
     * @return what was decided; empty if the application held no place on this board
     */
    public List<Decision> withdraw(Application application) {
        synchronized (lock) {
            List<Decision> decisions = new ArrayList<>(2);
            if (removeFrom(selected, application)) {
                application.setStatus(ApplicationStatus.WITHDRAWN);
                decisions.add(new Decision(AuditEvent.WITHDRAWN, application, "gave back a seat"));
                Application next = waitlist.poll();
                if (next != null) {
                    select(next, "promoted from waitlist after withdrawal by "
                            + application.student().rollNo(), decisions);
                }
            } else if (removeFrom(waitlist, application)) {
                application.setStatus(ApplicationStatus.WITHDRAWN);
                decisions.add(new Decision(AuditEvent.WITHDRAWN, application, "left the waitlist"));
            }
            return decisions;
        }
    }

    /**
     * Copies the current allocation under the lock.
     *
     * @return a consistent snapshot, both lists best-ranked first
     */
    public BoardSnapshot snapshot() {
        synchronized (lock) {
            return new BoardSnapshot(company, sortedCopy(selected), sortedCopy(waitlist));
        }
    }

    /**
     * Returns how many seats are currently taken.
     *
     * @return the number of selected applications
     */
    public int selectedCount() {
        synchronized (lock) {
            return selected.size();
        }
    }

    /** A PriorityQueue only knows its top element, so copy it into a list and sort that list. */
    private static List<Application> sortedCopy(PriorityQueue<Application> queue) {
        List<Application> list = new ArrayList<>(queue);
        list.sort(BEST_FIRST);
        return list;
    }

    private void select(Application application, String reason, List<Decision> decisions) {
        application.setStatus(ApplicationStatus.SELECTED);
        selected.add(application);
        decisions.add(new Decision(AuditEvent.SELECTED, application, reason));
    }

    /** Removes by identity using an explicit {@link Iterator}, because PriorityQueue has no keyed removal. */
    private static boolean removeFrom(PriorityQueue<Application> queue, Application target) {
        for (Iterator<Application> it = queue.iterator(); it.hasNext();) {
            if (it.next() == target) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
