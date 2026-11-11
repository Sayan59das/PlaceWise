package com.placewise.drive;

import com.placewise.audit.AuditEvent;
import com.placewise.audit.AuditLog;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentRanking;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Shared helpers for the drive tests. */
final class DriveFixtures {

    static final EligibilityChecker CHECKER = new EligibilityChecker();

    private DriveFixtures() {
    }

    /** Generates data for the config and runs a silent drive. */
    static DriveResult drive(DriveConfig config, AuditLog audit) {
        List<Student> students = DriveDataGenerator.students(config.students(), config.seed());
        List<Company> companies = DriveDataGenerator.companies(config.companies(), config.seatsPerCompany(),
                config.seed());
        return new PlacementDrive(students, companies, config, CHECKER, audit, null).run();
    }

    /**
     * A deliberately naive prediction of who must hold a seat, derived only from the applications and the
     * withdrawals that happened: the top {@code seats} eligible, non-withdrawn, distinct applicants by rank.
     * Returns, per company id, the roll numbers that must be SELECTED and those that must be WAITLISTED.
     */
    static Map<Long, List<Set<String>>> expectedAllocation(DriveResult result) {
        return result.companies().stream().collect(Collectors.toMap(Company::id, company -> {
            Set<String> withdrawn = result.applications().stream()
                    .filter(a -> a.company().id() == company.id() && a.status() == ApplicationStatus.WITHDRAWN)
                    .map(a -> a.student().rollNo()).collect(Collectors.toSet());
            List<Student> pool = result.applications().stream()
                    .filter(a -> a.company().id() == company.id())
                    .map(Application::student)
                    .filter(s -> CHECKER.isEligible(s, company))
                    .filter(s -> !withdrawn.contains(s.rollNo()))
                    .distinct()
                    .sorted(StudentRanking.ORDER)
                    .toList();
            Set<String> selected = new TreeSet<>();
            Set<String> waiting = new TreeSet<>();
            for (int i = 0; i < pool.size(); i++) {
                (i < company.seats() ? selected : waiting).add(pool.get(i).rollNo());
            }
            return List.of(selected, waiting);
        }));
    }

    static Set<String> rolls(List<Application> applications) {
        return applications.stream().map(a -> a.student().rollNo()).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Collects audit lines (event + thread + message) in a thread-safe list. */
    static final class RecordingAudit implements AuditLog {
        final List<String> lines = java.util.Collections.synchronizedList(new ArrayList<>());

        @Override
        public void log(com.placewise.audit.AuditEvent event, String message) {
            lines.add(event + "|" + Thread.currentThread().getName() + "|" + message);
        }

        long count(com.placewise.audit.AuditEvent event) {
            synchronized (lines) {
                return lines.stream().filter(l -> l.startsWith(event + "|")).count();
            }
        }
    }
}
