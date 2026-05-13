package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalAvailabilityService;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeReadinessView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PjbRuntimeReadinessService {

    private final PjbFunctionalAvailabilityService availabilityService;
    private final PjbRuntimePressureService runtimePressureService;
    private final PjbRuntimeDrainService runtimeDrainService;
    private final PjbLivePressureService livePressureService;
    private final PjbKafkaPressureService kafkaPressureService;
    private final PjbTransactionalPressureTracker transactionalPressureTracker;
    private final PjbRuntimePressureProperties properties;

    public PjbRuntimeReadinessService(PjbFunctionalAvailabilityService availabilityService,
                                      PjbRuntimePressureService runtimePressureService,
                                      PjbRuntimeDrainService runtimeDrainService,
                                      PjbLivePressureService livePressureService,
                                      PjbKafkaPressureService kafkaPressureService,
                                      PjbTransactionalPressureTracker transactionalPressureTracker,
                                      PjbRuntimePressureProperties properties) {
        this.availabilityService = availabilityService;
        this.runtimePressureService = runtimePressureService;
        this.runtimeDrainService = runtimeDrainService;
        this.livePressureService = livePressureService;
        this.kafkaPressureService = kafkaPressureService;
        this.transactionalPressureTracker = transactionalPressureTracker;
        this.properties = properties;
    }

    public PjbRuntimeReadinessView snapshot() {
        boolean functionalUp = functionalUp();
        PjbRuntimePressureService.Snapshot runtimePressure = runtimePressureService.snapshot();
        PjbRuntimeDrainService.Snapshot runtimeDrain = runtimeDrainService.snapshot();
        PjbLivePressureService.Snapshot livePressure = livePressureService.snapshot(runtimePressure.warmingUp());
        PjbKafkaPressureService.Snapshot kafkaPressure = kafkaPressureService.snapshot(runtimePressure.warmingUp());
        PjbTransactionPressureView transactions = transactionalPressureTracker.snapshot();
        boolean transactionCritical = transactionCritical(transactions);
        boolean up = functionalUp
                && runtimePressure.ready()
                && runtimeDrain.readyForTraffic()
                && !runtimePressure.criticalGcPressure()
                && !livePressure.criticalSurge()
                && !kafkaPressure.critical()
                && !transactionCritical;
        return new PjbRuntimeReadinessView(
                Instant.now(),
                up,
                status(functionalUp, runtimePressure, runtimeDrain, livePressure, kafkaPressure, transactions, up),
                functionalUp,
                runtimePressure,
                runtimeDrain,
                livePressure,
                kafkaPressure,
                transactions
        );
    }

    private boolean functionalUp() {
        for (PjbFunctionalDomain domain : PjbFunctionalDomain.values()) {
            if (!availabilityService.readiness(domain).available()) {
                return false;
            }
        }
        return true;
    }

    private boolean transactionCritical(PjbTransactionPressureView transactions) {
        boolean pressure = properties.isFailReadyOnCriticalTransactionPressure()
                && (transactions.activePressure() || transactions.longRunningPressure());
        boolean budget = properties.isFailReadyOnCriticalTransactionBudgetViolation()
                && transactions.budgetPressure();
        return pressure || budget;
    }

    private String status(boolean functionalUp,
                          PjbRuntimePressureService.Snapshot runtimePressure,
                          PjbRuntimeDrainService.Snapshot runtimeDrain,
                          PjbLivePressureService.Snapshot livePressure,
                          PjbKafkaPressureService.Snapshot kafkaPressure,
                          PjbTransactionPressureView transactions,
                          boolean up) {
        if (runtimeDrain.draining()) {
            return "DRAINING";
        }
        if (!functionalUp) {
            return "FUNCTIONAL_DEGRADED";
        }
        if (runtimePressure.warmingUp()) {
            return "WARMING_UP";
        }
        if (runtimePressure.criticalMemoryRunaway()) {
            return "MEMORY_RUNAWAY";
        }
        if (runtimePressure.criticalGcPressure()) {
            return "GC_PRESSURE";
        }
        if (runtimePressure.criticalDatasourceRunaway()) {
            return "DATASOURCE_RUNAWAY";
        }
        if (properties.isFailReadyOnCriticalTransactionBudgetViolation() && transactions.budgetPressure()) {
            return "TRANSACTION_BUDGET_PRESSURE";
        }
        if (properties.isFailReadyOnCriticalTransactionPressure() && (transactions.activePressure() || transactions.longRunningPressure())) {
            return "TRANSACTION_PRESSURE";
        }
        if (livePressure.criticalSurge()) {
            return "LIVE_SURGE";
        }
        if (kafkaPressure.critical()) {
            return "KAFKA_BACKPRESSURED";
        }
        if (runtimePressure.schedulerTrendingUp()) {
            return "SCHEDULER_TRENDING_UP";
        }
        if (runtimePressure.trend().risingFast()) {
            return "PRESSURE_TRENDING_UP";
        }
        if (transactions.budgetPressure()) {
            return "TRANSACTION_BUDGET_WATCH";
        }
        if (transactions.longRunningPressure()) {
            return "TRANSACTION_LONG_RUNNING";
        }
        if (transactions.activePressure()) {
            return "TRANSACTION_ACTIVE_PRESSURE";
        }
        return up ? "UP" : "PRESSURED";
    }
}
