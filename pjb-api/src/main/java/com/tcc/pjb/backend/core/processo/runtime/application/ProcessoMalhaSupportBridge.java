package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ProcessoMalhaSupportBridge {

    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoMalhaSupportBridge(ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                      ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
    }

    public DecisionTraceService decisionTraceService() {
        return decisionTraceService;
    }

    public AuditLedgerService auditLedgerService() {
        return auditLedgerService;
    }

    public boolean possuiDecisionTrace() {
        return Objects.nonNull(decisionTraceService);
    }

    public boolean possuiAuditLedger() {
        return Objects.nonNull(auditLedgerService);
    }
}
