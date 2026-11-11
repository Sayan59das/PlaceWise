package com.placewise.drive;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.model.ApplicationStatus;
import com.placewise.ranking.StudentRanking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

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
        // Group the applications by (student, company) pair.
        Map<ApplicationKey, List<Application>> byPair = new HashMap<>();
        for (Application application : result.applications()) {
            List<Application> group = byPair.get(application.key());
            if (group == null) {
                group = new ArrayList<>();
                byPair.put(application.key(), group);
            }
            group.add(application);
        }
        for (Application application : result.applications()) {
            if (application.status() == ApplicationStatus.APPLIED) {
                violations.add(application + " never reached a final status");
            }
            boolean eligible = checker.isEligible(application.student(), application.company());
            if (LIVE.contains(application.status()) && !eligible) {
                violations.add(application + " but the student is not eligible");
            }
            if (application.status() == ApplicationStatus.REJECTED && eligible
                    && countLive(byPair.get(application.key())) == 0) {
                violations.add(application + " was rejected although eligible and not a duplicate");
            }
        }
        for (Map.Entry<ApplicationKey, List<Application>> entry : byPair.entrySet()) {
            ApplicationKey key = entry.getKey();
            int live = countLive(entry.getValue());
            int selected = 0;
            for (Application application : entry.getValue()) {
                if (application.status() == ApplicationStatus.SELECTED) {
                    selected++;
                }
            }
            if (selected > 1) {
                violations.add(key.rollNo() + " selected " + selected + " times at company " + key.companyId());
            }
            if (live > 1) {
                violations.add(key.rollNo() + " holds " + live + " live applications at company " + key.companyId());
            }
        }
    }

    /** How many applications in the list are still "live" (selected or waitlisted). */
    private static int countLive(List<Application> applications) {
        int live = 0;
        for (Application application : applications) {
            if (LIVE.contains(application.status())) {
                live++;
            }
        }
        return live;
    }

    /** The boards (what the allocator believes) and the application objects (what students see) must agree. */
    private static void checkBoardsMatchStatuses(DriveResult result, List<String> violations) {
        Set<Application> onBoardSelected = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Application> onBoardWaiting = Collections.newSetFromMap(new IdentityHashMap<>());
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
