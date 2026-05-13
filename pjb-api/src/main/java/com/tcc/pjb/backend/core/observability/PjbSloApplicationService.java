package com.tcc.pjb.backend.core.observability;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.domain.PjbSloBudgetAuditView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloBudgetHealthView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloBudgetView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloConsistencyView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloExecutionQuery;
import com.tcc.pjb.backend.core.observability.domain.PjbSloExecutionResult;
import com.tcc.pjb.backend.core.observability.domain.PjbSloLatencyAuditView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloLatencyWindowView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloOperationAuditView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloOperationHealthView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloOperationQuery;
import com.tcc.pjb.backend.core.observability.domain.PjbSloOperationResult;
import com.tcc.pjb.backend.core.observability.domain.PjbSloRegistryAuditView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloRegistryHealthView;
import com.tcc.pjb.backend.core.observability.domain.PjbSloRegistrySnapshot;
import com.tcc.pjb.backend.core.observability.domain.PjbSloTimelineResult;
import com.tcc.pjb.backend.core.observability.domain.PjbSloViolationSnapshot;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSloApplicationService {

    private final PjbSloRegistry registry;
    private final AuditLedgerService auditLedgerService;

    public PjbSloApplicationService(PjbSloRegistry registry, AuditLedgerService auditLedgerService) {
        this.registry = Objects.requireNonNull(registry);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    public PjbSloRegistrySnapshot registrySnapshot() {
        return registry.snapshot();
    }

    public PjbSloRegistryHealthView registryHealth() {
        return registry.registryHealthView();
    }

    public PjbSloRegistryAuditView registryAudit() {
        PjbSloRegistrySnapshot snapshot = registry.snapshot();
        int total = snapshot.operations().size();
        return new PjbSloRegistryAuditView(
                "pjb-slo-registry",
                total > 0 ? "AVAILABLE" : "EMPTY",
                snapshot.capturedAt());
    }

    public PjbSloOperationResult operation(String operation) {
        return registry.operation(new PjbSloOperationQuery(normalizeOperation(operation)));
    }

    public PjbSloOperationHealthView operationHealth(String operation) {
        return registry.healthView(normalizeOperation(operation));
    }

    public PjbSloBudgetView budget(String operation) {
        return registry.budgetView(normalizeOperation(operation));
    }

    public PjbSloBudgetHealthView budgetHealth(String operation) {
        String normalized = normalizeOperation(operation);
        PjbSloOperationHealthView healthView = registry.healthView(normalized);
        return new PjbSloBudgetHealthView(
                normalized,
                healthView.targetSeconds(),
                healthView.registered() && healthView.targetSeconds() > 0,
                healthView.registered()
                        ? "budget operacional carregado"
                        : "operacao sem budget registrado");
    }

    public PjbSloBudgetAuditView budgetAudit(String operation) {
        PjbSloBudgetHealthView budgetHealthView = budgetHealth(operation);
        return new PjbSloBudgetAuditView(
                budgetHealthView.operation(),
                budgetHealthView.healthy() ? "HEALTHY" : "MISSING",
                budgetHealthView.summary());
    }

    public PjbSloLatencyWindowView latencyWindow(String operation) {
        return registry.latencyWindowView(normalizeOperation(operation));
    }

    public PjbSloLatencyAuditView latencyAudit(String operation) {
        PjbSloLatencyWindowView view = latencyWindow(operation);
        return new PjbSloLatencyAuditView(
                view.operation(),
                view.targetSeconds() > 0 ? "AVAILABLE" : "EMPTY",
                view.targetSeconds() > 0
                        ? "janela de latencia registrada"
                        : "operacao sem janela de latencia declarada");
    }

    public PjbSloTimelineResult timeline(String operation, String event) {
        String normalized = normalizeOperation(operation);
        String normalizedEvent = normalizeEvent(event);
        auditLedgerService.appendSafely(
                "PJB_SLO_TIMELINE_QUERY",
                "SLO",
                normalized,
                null,
                "event=" + normalizedEvent);
        return registry.timeline(normalized, normalizedEvent);
    }

    public PjbSloOperationAuditView operationAudit(String operation) {
        PjbSloOperationResult result = operation(operation);
        return new PjbSloOperationAuditView(
                result.operation(),
                result.exists() ? "REGISTERED" : "MISSING",
                Instant.now());
    }

    public PjbSloConsistencyView consistency(String operation) {
        String normalized = normalizeOperation(operation);
        PjbSloOperationResult result = operation(normalized);
        PjbSloBudgetView budgetView = budget(normalized);
        boolean consistent = result.exists() == (budgetView.targetSeconds() > 0);
        return new PjbSloConsistencyView(
                normalized,
                consistent,
                consistent ? "operation e budget coerentes" : "drift entre operation e budget",
                "pjb-slo-registry");
    }

    public PjbSloExecutionResult evaluate(PjbSloExecutionQuery query) {
        String normalized = normalizeOperation(query.operation());
        double measuredSeconds = normalizeMeasuredSeconds(query.measuredSeconds());
        double sloSeconds = budget(normalized).targetSeconds();
        boolean violated = sloSeconds > 0 && measuredSeconds > sloSeconds;
        auditLedgerService.appendSafely(
                violated ? "PJB_SLO_VIOLATION" : "PJB_SLO_MEASURED",
                "SLO",
                normalized,
                null,
                "measuredSeconds=" + measuredSeconds + " sloSeconds=" + sloSeconds);
        return new PjbSloExecutionResult(normalized, measuredSeconds, sloSeconds, violated);
    }

    public PjbSloViolationSnapshot violationSnapshot(String operation, double measuredSeconds) {
        PjbSloExecutionResult result = evaluate(new PjbSloExecutionQuery(operation, measuredSeconds));
        return new PjbSloViolationSnapshot(
                result.operation(),
                result.sloSeconds(),
                result.measuredSeconds(),
                Instant.now());
    }

    private String normalizeOperation(String operation) {
        String value = Objects.requireNonNull(operation, "operation obrigatoria").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("operation obrigatoria");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String normalizeEvent(String event) {
        if (event == null || event.isBlank()) {
            return "queried";
        }
        return event.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private double normalizeMeasuredSeconds(double measuredSeconds) {
        if (Double.isNaN(measuredSeconds) || Double.isInfinite(measuredSeconds) || measuredSeconds < 0) {
            throw new IllegalArgumentException("measuredSeconds invalido");
        }
        return measuredSeconds;
    }
}
