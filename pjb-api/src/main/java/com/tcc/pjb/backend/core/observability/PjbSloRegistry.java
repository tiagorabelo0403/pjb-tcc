package com.tcc.pjb.backend.core.observability;

import com.tcc.pjb.backend.core.observability.domain.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PjbSloRegistry {
    private static final List<PjbSloLatencyBudget> DEFAULT_BUDGETS = List.of(
            new PjbSloLatencyBudget("peticionamento", 3.0),
            new PjbSloLatencyBudget("busca_processual", 0.5),
            new PjbSloLatencyBudget("protocolo_assinatura", 5.0),
            new PjbSloLatencyBudget("painel_cidadao", 1.0),
            new PjbSloLatencyBudget("painel_magistrado", 1.0),
            new PjbSloLatencyBudget("datajud_feed_batch", 30.0),
            new PjbSloLatencyBudget("mni_remessa", 10.0),
            new PjbSloLatencyBudget("sisbajud_bloqueio", 10.0),
            new PjbSloLatencyBudget("icp_chain_validation", 2.0)
    );
    private final MeterRegistry registry;
    public PjbSloRegistry(MeterRegistry registry) { this.registry = registry; }
    @PostConstruct void registerSlos() { for (PjbSloLatencyBudget budget : DEFAULT_BUDGETS) { registerOperationTimer(budget.operation(), budget.targetSeconds()); } }
    private void registerOperationTimer(String operation, double sloSeconds) {
        Timer.builder("pjb.op.latency").tag("operation", operation).description("Latência P99 alvo: " + sloSeconds + "s").sla(Duration.ofMillis((long) (sloSeconds * 1000))).publishPercentiles(0.50, 0.95, 0.99).publishPercentileHistogram().register(registry);
    }
    public Timer timer(String operation) { return registry.find("pjb.op.latency").tag("operation", operation).timer(); }
    public PjbSloCheckResult check(PjbSloCheckCommand command) { Timer timer = timer(command.operation()); return new PjbSloCheckResult(command.operation(), timer != null, timer == null ? 0L : timer.count()); }
    public PjbSloOperationView operationView(String operation) { Timer timer = timer(operation); return new PjbSloOperationView(operation, timer != null, timer == null ? 0L : timer.count()); }
    public PjbSloRegistrySnapshot snapshot() { return new PjbSloRegistrySnapshot(DEFAULT_BUDGETS.stream().map(PjbSloLatencyBudget::operation).map(this::operationView).toList(), Instant.now()); }
    public PjbSloAuditEntry auditEntry(String operation, String event) { return new PjbSloAuditEntry(operation, event, Instant.now()); }
    public PjbSloHealthResult health(PjbSloHealthQuery query) { Timer timer = timer(query.operation()); return new PjbSloHealthResult(query.operation(), timer != null, timer == null ? 0L : timer.count(), Instant.now()); }
    public PjbSloOperationHealthView healthView(String operation) { PjbSloLatencyBudget budget = budget(operation); return new PjbSloOperationHealthView(operation, budget != null, budget == null ? 0.0 : budget.targetSeconds()); }
    public PjbSloLatencyWindowView latencyWindowView(String operation) { PjbSloLatencyBudget budget = budget(operation); return new PjbSloLatencyWindowView(operation, budget == null ? 0.0 : budget.targetSeconds(), Instant.now()); }
    public PjbSloBudgetView budgetView(String operation) { PjbSloLatencyBudget budget = budget(operation); return new PjbSloBudgetView(operation, budget == null ? 0.0 : budget.targetSeconds()); }
    public PjbSloTimelineResult timeline(String operation, String event) { Instant now = Instant.now(); return new PjbSloTimelineResult(List.of(new PjbSloTimelineEntry(operation, "received", now), new PjbSloTimelineEntry(operation, event, now))); }
    public PjbSloRegistryHealthView registryHealthView() { return new PjbSloRegistryHealthView(true, DEFAULT_BUDGETS.size(), Instant.now()); }
    public PjbSloOperationResult operation(PjbSloOperationQuery query) { Timer timer = timer(query.operation()); return new PjbSloOperationResult(query.operation(), timer != null, timer == null ? 0L : timer.count()); }
    private PjbSloLatencyBudget budget(String operation) { return DEFAULT_BUDGETS.stream().filter(it -> it.operation().equals(operation)).findFirst().orElse(null); }
}
