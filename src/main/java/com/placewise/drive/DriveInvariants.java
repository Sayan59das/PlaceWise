package com.placewise.drive;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.model.ApplicationStatus;
import com.placewise.ranking.StudentRanking;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Properties that must hold after <em>every</em> drive, whatever the thread timing was. Checking
 * invariants (instead of one expected output) is how concurrent code is tested: timing varies, truths do not.
 */
public final class DriveInvariants {

    private static final Set<ApplicationStatus> LIVE =
            EnumSet.of(ApplicationStatus.SELECTED, ApplicationStatus.WAITLISTED, ApplicationStatus.WITHDRAWN);

    private DriveInvariants() {
    }

    /**
     * Checks a finished drive.
     *
     * @param result  the drive result
     * @param checker the rules that decide eligibility
     * @return one message per violation; an empty list means every invariant holds
     */
    public static List<String> check(DriveResult result, EligibilityChecker checker) {
        List<String> violations = new ArrayList<>();
        checkBoards(result, violations);
        checkApplications(result, checker, violations);
        checkBoardsMatchStatuses(result, violations);
        return violations;
    }

    private static void checkBoards(DriveResult result, List<String> violations) {
        for (BoardSnapshot board : result.boards()) {
            String name = board.company().name();
            int seats = board.company().seats();
            if (board.selected().size() > seats) {
                violations.add(name + ": " + board.selected().size() + " selected but only " + seats + " seats");
            }
            if (!board.waitlisted().isEmpty() && board.selected().size() < seats) {
                violations.add(name + ": has a free seat while students are waitlisted");
            }
            if (!board.selected().isEmpty() && !board.waitlisted().isEmpty()) {
                Application worstSelected = board.selected().get(board.selected().size() - 1);
                Application bestWaiting = board.waitlisted().get(0);
                if (StudentRanking.ORDER.compare(worstSelected.student(), bestWaiting.student()) >= 0) {
                    violations.add(name + ": waitlisted " + bestWaiting.student().rollNo()
                            + " outranks selected " + worstSelected.student().rollNo());
                }
            }
        }
    }

    private static void checkApplications(DriveResult result, EligibilityChecker checker, List<String> violations) {
        Map<ApplicationKey, List<Application>> byPair = groupByPair(result.applications());
        for (Application application : result.applications()) {
            checkOneApplication(application, byPair.get(application.key()), checker, violations);
        }
        for (Map.Entry<ApplicationKey, List<Application>> entry : byPair.entrySet()) {
            checkOnePair(entry.getKey(), entry.getValue(), violations);
        }
    }

    /** Puts the applications of one student at one company into one group (a duplicate lands in the same group). */
    private static Map<ApplicationKey, List<Application>> groupByPair(List<Application> applications) {
        Map<ApplicationKey, List<Application>> byPair = new HashMap<>();
        for (Application application : applications) {
            List<Application> group = byPair.get(application.key());
            if (group == null) {
                group = new ArrayList<>();
                byPair.put(application.key(), group);
            }
            group.add(application);
        }
        return byPair;
    }

    /** Rules for one application; {@code samePair} holds every application of that student at that company. */
    private static void checkOneApplication(Application application, List<Application> samePair,
                                            EligibilityChecker checker, List<String> violations) {
        if (application.status() == ApplicationStatus.APPLIED) {
            violations.add(application + " never reached a final status");
        }
        boolean eligible = checker.isEligible(application.student(), application.company());
        if (LIVE.contains(application.status()) && !eligible) {
            violations.add(application + " but the student is not eligible");
        }
        if (application.status() == ApplicationStatus.REJECTED && eligible && countLive(samePair) == 0) {
            violations.add(application + " was rejected although eligible and not a duplicate");
        }
    }

    /** Rules for all applications of one student at one company. */
    private static void checkOnePair(ApplicationKey key, List<Application> applications, List<String> violations) {
        int selected = countWithStatus(applications, ApplicationStatus.SELECTED);
        int live = countLive(applications);
        if (selected > 1) {
            violations.add(key.rollNo() + " selected " + selected + " times at company " + key.companyId());
        }
        if (live > 1) {
            violations.add(key.rollNo() + " holds " + live + " live applications at company " + key.companyId());
        }
    }

    /** How many applications in the list are "live": selected, waitlisted, or withdrawn after having been live. */
    private static int countLive(List<Application> applications) {
        int live = 0;
        for (Application application : applications) {
            if (LIVE.contains(application.status())) {
                live++;
            }
        }
        return live;
    }

    private static int countWithStatus(List<Application> applications, ApplicationStatus status) {
        int count = 0;
        for (Application application : applications) {
            if (application.status() == status) {
                count++;
            }
        }
        return count;
    }

    /** The boards (what the allocator believes) and the application objects (what students see) must agree. */
    private static void checkBoardsMatchStatuses(DriveResult result, List<String> violations) {
        // Application does not override equals(), so a HashSet tells two applications apart by identity.
        Set<Application> onBoardSelected = new HashSet<>();
        Set<Application> onBoardWaiting = new HashSet<>();
        for (BoardSnapshot board : result.boards()) {
            onBoardSelected.addAll(board.selected());
            onBoardWaiting.addAll(board.waitlisted());
        }
        for (Application application : result.applications()) {
            boolean selected = application.status() == ApplicationStatus.SELECTED;
            boolean waiting = application.status() == ApplicationStatus.WAITLISTED;
            if (selected != onBoardSelected.contains(application)) {
                violations.add(application + ": status and company board disagree about the seat");
            }
            if (waiting != onBoardWaiting.contains(application)) {
                violations.add(application + ": status and company board disagree about the waitlist");
            }
        }
    }
}
