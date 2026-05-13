package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionGovernanceView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionLaneView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionOperationView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeGuardrailFinding;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeGuardrailsSnapshot;
import com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PjbRuntimeGuardrailsService {

    private final PjbExecutionOrchestrator executionOrchestrator;
    private final PjbRuntimePressureService runtimePressureService;
    private final PjbTransactionalPressureTracker transactionalPressureTracker;

    public PjbRuntimeGuardrailsService(PjbExecutionOrchestrator executionOrchestrator,
                                       PjbRuntimePressureService runtimePressureService,
                                       PjbTransactionalPressureTracker transactionalPressureTracker) {
        this.executionOrchestrator = executionOrchestrator;
        this.runtimePressureService = runtimePressureService;
        this.transactionalPressureTracker = transactionalPressureTracker;
    }

    public PjbRuntimeGuardrailsSnapshot snapshot() {
        PjbRuntimeExecutionGovernanceView execution = executionOrchestrator.snapshot();
        PjbRuntimePressureService.Snapshot pressure = runtimePressureService.snapshot();
        PjbTransactionPressureView transactions = transactionalPressureTracker.snapshot();
        ArrayList<PjbRuntimeGuardrailFinding> findings = new ArrayList<>();

        if (!pressure.criticalOverloadedExecutorNames().isEmpty()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "CRITICAL_LANE_OVERLOAD",
                    "critical",
                    "execution",
                    "Há lanes críticas saturadas: " + String.join(", ", pressure.criticalOverloadedExecutorNames()),
                    "Reduzir fan-out assíncrono, revisar operações longas e migrar submissões soltas para o orquestrador soberano."
            ));
        }
        if (pressure.criticalDatasourceRunaway()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "DATASOURCE_RUNAWAY",
                    "critical",
                    "database",
                    "Há pool de banco crítico com pressão sustentada, headroom apertado ou fila crescente.",
                    "Investigar conexões longas, transações abertas demais, N+1 e desacoplar chamadas remotas do escopo transacional."
            ));
        }
        if (pressure.schedulerTrendingUp() || pressure.schedulerSustainedPressure()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "TIMEOUT_SCHEDULER_PRESSURE",
                    pressure.schedulerSustainedPressure() ? "high" : "medium",
                    "scheduler",
                    "O scheduler de timeouts está com fila crescente ou pressão sustentada.",
                    "Rever explosão de timeouts curtos, rajadas de fan-out e operações que estouram budget sem backpressure."
            ));
        }
        if (pressure.criticalMemoryRunaway()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "MEMORY_RUNAWAY",
                    "critical",
                    "memory",
                    "Há pressão crítica de heap, metaspace ou direct buffer com tendência de crescimento sustentado.",
                    "Inspecionar retenção de coleções, caches sem limite e payloads grandes mantidos além do necessário."
            ));
        }
        if (pressure.criticalGcPressure()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "GC_PRESSURE",
                    "high",
                    "gc",
                    "A coleta de lixo está indicando pressão crítica para o papel atual do componente.",
                    "Reduzir churn de objetos, cortar buffers temporários e revisar agregações que materializam listas excessivas."
            ));
        }
        if (transactions.activePressure()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "TRANSACTION_ACTIVE_PRESSURE",
                    "high",
                    "transaction",
                    "Há pressão de concorrência transacional acima do orçamento operacional.",
                    "Fatiar operações longas, remover chamadas remotas do escopo transacional e revisar retenção de conexão nas trilhas críticas."
            ));
        }
        if (transactions.longRunningPressure()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "TRANSACTION_LONG_RUNNING",
                    "high",
                    "transaction",
                    "Há transações longas observadas na malha de execução.",
                    "Separar leitura, inferência externa e persistência em fronteiras transacionais curtas e explícitas."
            ));
        }
        if (transactions.budgetPressure()) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "TRANSACTION_BUDGET_PRESSURE",
                    transactions.criticalBudgetViolations() > 0L ? "critical" : "high",
                    "transaction",
                    "Há violações de orçamento transacional por operação crítica ou recorrente.",
                    "Encerrar transações antes de IA/chamada remota, aplicar budget explícito e mover persistência para fase curta de commit."
            ));
        }

        long timedOutCritical = execution.operations().stream()
                .filter(operation -> operation.critical() && operation.timedOutTasks() > 0L)
                .count();
        if (timedOutCritical > 0L) {
            findings.add(new PjbRuntimeGuardrailFinding(
                    "CRITICAL_OPERATION_TIMEOUTS",
                    "high",
                    "execution",
                    "Há operações críticas com timeout acumulado no orquestrador de execução.",
                    "Rever budget temporal, lane escolhida e chamadas externas que estão retornando tarde demais."
            ));
        }

        int executionHeadroom = computeExecutionHeadroom(execution);
        int datasourceHeadroom = computeDatasourceHeadroom(pressure);
        int memoryHeadroom = computeMemoryHeadroom(pressure);
        int gcHeadroom = computeGcHeadroom(pressure);
        int transactionHeadroom = computeTransactionHeadroom(transactions);
        int riskScore = Math.min(100, Math.max(0, 100 - pressure.headroomScore()) + findings.size() * 7);
        boolean healthy = pressure.ready() && findings.stream().noneMatch(f -> "critical".equalsIgnoreCase(f.severity()));

        return new PjbRuntimeGuardrailsSnapshot(
                Instant.now(),
                healthy,
                riskScore,
                pressure.headroomScore(),
                executionHeadroom,
                datasourceHeadroom,
                memoryHeadroom,
                gcHeadroom,
                transactionHeadroom,
                List.copyOf(findings)
        );
    }

    private int computeExecutionHeadroom(PjbRuntimeExecutionGovernanceView execution) {
        double averageUtilization = execution.lanes().stream()
                .mapToDouble(PjbRuntimeExecutionLaneView::utilizationRatio)
                .average()
                .orElse(0.0d);
        long activeHotspots = execution.operations().stream().filter(op -> op.activeTasks() > 0 && op.maxObservedConcurrency() >= 4).count();
        int score = Math.max(0, 100 - (int) Math.round(averageUtilization * 100.0d));
        return Math.max(0, score - (int) Math.min(30L, activeHotspots * 5L));
    }

    private int computeDatasourceHeadroom(PjbRuntimePressureService.Snapshot pressure) {
        if (pressure.dataSources().isEmpty()) {
            return 100;
        }
        double avgUsage = pressure.dataSources().stream().mapToDouble(PjbRuntimePressureService.DatasourcePressure::usageRatio).average().orElse(0.0d);
        int score = Math.max(0, 100 - (int) Math.round(avgUsage * 100.0d));
        if (pressure.criticalDatasourceRunaway()) {
            score = Math.max(0, score - 35);
        }
        return score;
    }

    private int computeMemoryHeadroom(PjbRuntimePressureService.Snapshot pressure) {
        PjbRuntimePressureService.MemoryPressure memory = pressure.memory();
        if (memory == null) {
            return 100;
        }
        int score = Math.max(0, 100 - (int) Math.round(memory.heapUsageRatio() * 100.0d));
        if (memory.criticalRunaway()) {
            score = Math.max(0, score - 30);
        }
        return score;
    }

    private int computeGcHeadroom(PjbRuntimePressureService.Snapshot pressure) {
        PjbRuntimePressureService.GcPressure gc = pressure.gc();
        if (gc == null) {
            return 100;
        }
        int score = gc.critical() ? 35 : gc.sustained() ? 55 : 85;
        return Math.max(0, Math.min(100, score));
    }

    private int computeTransactionHeadroom(PjbTransactionPressureView transactions) {
        if (transactions == null) {
            return 100;
        }
        int score = 100;
        if (transactions.activePressure()) {
            score -= 35;
        }
        if (transactions.longRunningPressure()) {
            score -= 25;
        }
        if (transactions.budgetPressure()) {
            score -= 20;
        }
        long hotspots = transactions.operations().stream()
                .filter(operation -> operation.longRunningTransactions() > 0L || operation.activeTransactions() > 0)
                .count();
        score -= (int) Math.min(30L, hotspots * 5L);
        return Math.max(0, score);
    }
}
