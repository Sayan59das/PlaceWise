package com.placewise.drive;

import com.placewise.audit.AuditLog;
import com.placewise.model.Application;
import java.util.List;
import java.util.Map;

/** Processes a slice of withdrawals; several workers run at once and may hit the same company. */
public final class WithdrawalWorker implements Runnable {

    private final List<Application> withdrawals;
    private final Map<Long, CompanyBoard> boards;
    private final AuditLog audit;

    WithdrawalWorker(List<Application> withdrawals, Map<Long, CompanyBoard> boards, AuditLog audit) {
        this.withdrawals = withdrawals;
        this.boards = boards;
        this.audit = audit;
    }

    @Override
    public void run() {
        for (Application application : withdrawals) {
            for (Decision decision : boards.get(application.company().id()).withdraw(application)) {
                audit.log(decision.event(), decision.message());
            }
        }
    }
}
