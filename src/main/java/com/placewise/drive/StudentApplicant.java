package com.placewise.drive;

import com.placewise.audit.AuditEvent;
import com.placewise.audit.AuditLog;
import com.placewise.concurrency.BoundedQueue;
import com.placewise.concurrency.StartGate;
import com.placewise.model.Application;
import java.util.List;

/**
 * A producer: submits its students' planned applications into the shared queue when the gate opens.
 * {@code put} blocks while the queue is full, which is the back-pressure that keeps producers from
 * outrunning the recruiters.
 */
public final class StudentApplicant implements Runnable {

    private final List<Application> plan;
    private final BoundedQueue<Application> queue;
    private final AuditLog audit;
    private final ProgressCounter progress;
    private final StartGate gate;

    StudentApplicant(List<Application> plan, BoundedQueue<Application> queue, AuditLog audit,
                     ProgressCounter progress, StartGate gate) {
        this.plan = plan;
        this.queue = queue;
        this.audit = audit;
        this.progress = progress;
        this.gate = gate;
    }

    @Override
    public void run() {
        try {
            gate.awaitOpen();
            for (Application application : plan) {
                audit.log(AuditEvent.APPLICATION, application.student().rollNo() + " -> "
                        + application.company().name());
                queue.put(application);
                progress.submitted();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the flag and stop: we were asked to shut down
        }
    }
}
