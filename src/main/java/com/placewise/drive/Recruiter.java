package com.placewise.drive;

import com.placewise.audit.AuditEvent;
import com.placewise.audit.AuditLog;
import com.placewise.concurrency.BoundedQueue;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.eligibility.EligibilityResult;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import java.util.Map;
import java.util.Optional;

/**
 * A consumer: takes applications from the shared queue, checks eligibility, and hands eligible ones to
 * the right {@link CompanyBoard}. It stops when the queue is closed and empty.
 *
 * <p>The {@code boards} map is filled before any thread starts and never changed afterwards, and
 * {@link Thread#start()} publishes it safely, so reading it without a lock is fine.
 */
public final class Recruiter implements Runnable {

    private final BoundedQueue<Application> queue;
    private final Map<Long, CompanyBoard> boards;
    private final EligibilityChecker checker;
    private final AuditLog audit;
    private final ProgressCounter progress;

    Recruiter(BoundedQueue<Application> queue, Map<Long, CompanyBoard> boards, EligibilityChecker checker,
              AuditLog audit, ProgressCounter progress) {
        this.queue = queue;
        this.boards = boards;
        this.checker = checker;
        this.audit = audit;
        this.progress = progress;
    }

    @Override
    public void run() {
        try {
            Optional<Application> next = queue.take();
            while (next.isPresent()) {
                process(next.get());
                progress.processed();
                next = queue.take();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void process(Application application) {
        EligibilityResult result = checker.check(application.student(), application.company());
        audit.log(AuditEvent.ELIGIBILITY_CHECK, application.student().rollNo() + " vs "
                + application.company().name() + " -> " + (result.eligible() ? "ELIGIBLE" : "NOT ELIGIBLE"));
        if (!result.eligible()) {
            application.setStatus(ApplicationStatus.REJECTED);
            audit.log(AuditEvent.REJECTED, application.student().rollNo() + " -> "
                    + application.company().name() + ": " + String.join("; ", result.failedReasons()));
            return;
        }
        // The board's lock is released when admit() returns; only then do we touch the audit log's lock.
        for (Decision decision : boards.get(application.company().id()).admit(application)) {
            audit.log(decision.event(), decision.message());
        }
    }
}
