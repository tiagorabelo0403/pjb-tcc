package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeReadinessView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("pjbRuntimePressure")
public class PjbRuntimePressureHealthIndicator implements HealthIndicator {

    private final PjbRuntimeReadinessService readinessService;

    public PjbRuntimePressureHealthIndicator(PjbRuntimeReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @Override
    public Health health() {
        PjbRuntimeReadinessView readiness = readinessService.snapshot();
        PjbRuntimePressureService.Snapshot snapshot = readiness.runtimePressure();
        PjbRuntimeDrainService.Snapshot drain = readiness.runtimeDrain();
        PjbLivePressureService.Snapshot live = readiness.livePressure();
        PjbKafkaPressureService.Snapshot kafka = readiness.kafkaPressure();
        PjbTransactionPressureView transactions = readiness.transactions();
        Health.Builder builder;
        if (drain.draining() || snapshot.warmingUp()) {
            builder = Health.outOfService();
        } else if (!readiness.up()) {
            builder = Health.down();
        } else {
            builder = Health.up();
        }
        return builder
                .withDetail("status", readiness.status())
                .withDetail("componentRole", snapshot.componentRole())
                .withDetail("availableProcessors", snapshot.availableProcessors())
                .withDetail("maxMemoryMiB", snapshot.maxMemoryMiB())
                .withDetail("pressureScore", snapshot.pressureScore())
                .withDetail("headroomScore", snapshot.headroomScore())
                .withDetail("warmingUp", snapshot.warmingUp())
                .withDetail("uptimeMillis", snapshot.uptimeMillis())
                .withDetail("overloadedExecutors", snapshot.overloadedExecutorNames())
                .withDetail("degradedDatasources", snapshot.degradedDatasourceNames())
                .withDetail("risingDatasources", snapshot.risingDatasourceNames())
                .withDetail("criticalTightDatasources", snapshot.criticalTightDatasourceNames())
                .withDetail("memoryPressure", memoryPressure(snapshot.memory()))
                .withDetail("gcPressure", gcPressure(snapshot.gc()))
                .withDetail("executorPressure", overloadedExecutors(snapshot.executors()))
                .withDetail("datasourcePressure", degradedDataSources(snapshot.dataSources()))
                .withDetail("schedulerPressure", schedulerPressure(snapshot.scheduler()))
                .withDetail("transactions", transactionPressure(transactions))
                .withDetail("alerts", alerts(snapshot.alerts()))
                .withDetail("criticalOverloadedExecutors", snapshot.criticalOverloadedExecutorNames())
                .withDetail("criticalMemoryRunaway", snapshot.criticalMemoryRunaway())
                .withDetail("criticalGcPressure", snapshot.criticalGcPressure())
                .withDetail("trend", snapshot.trend())
                .withDetail("livePressure", livePressure(live))
                .withDetail("kafkaPressure", kafkaPressure(kafka))
                .withDetail("draining", drain.draining())
                .withDetail("drainAgeMillis", drain.drainAgeMillis())
                .withDetail("drainReason", drain.reason())
                .build();
    }

    private List<Map<String, Object>> overloadedExecutors(List<PjbRuntimePressureService.ExecutorPressure> executors) {
        return executors.stream().filter(PjbRuntimePressureService.ExecutorPressure::overloaded).limit(5).map(executor -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("beanName", executor.beanName());
            details.put("laneName", executor.laneName());
            details.put("threadNamePrefix", executor.threadNamePrefix());
            details.put("activeTasks", executor.activeTasks());
            details.put("concurrencyLimit", executor.concurrencyLimit());
            details.put("utilizationRatio", executor.utilizationRatio());
            details.put("averageAcquireWaitMillis", executor.averageAcquireWaitMillis());
            details.put("saturationRejections", executor.saturationRejections());
            details.put("consecutiveRejections", executor.consecutiveRejections());
            details.put("acceptingTasks", executor.acceptingTasks());
            details.put("lastRejectedAt", executor.lastRejectedAt());
            details.put("criticalLane", executor.criticalLane());
            return details;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> degradedDataSources(List<PjbRuntimePressureService.DatasourcePressure> dataSources) {
        return dataSources.stream().filter(PjbRuntimePressureService.DatasourcePressure::degraded).limit(5).map(dataSource -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("beanName", dataSource.beanName());
            details.put("poolRole", dataSource.poolRole());
            details.put("activeConnections", dataSource.activeConnections());
            details.put("awaitingThreads", dataSource.awaitingThreads());
            details.put("maximumPoolSize", dataSource.maximumPoolSize());
            details.put("budgetCeiling", dataSource.budgetCeiling());
            details.put("headroom", dataSource.headroom());
            details.put("usageRatio", dataSource.usageRatio());
            details.put("criticalPool", dataSource.criticalPool());
            details.put("headroomTight", dataSource.headroomTight());
            details.put("awaitingDelta", dataSource.awaitingDelta());
            details.put("usageDelta", dataSource.usageDelta());
            details.put("risingFast", dataSource.risingFast());
            details.put("sustained", dataSource.sustained());
            return details;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> schedulerPressure(PjbRuntimePressureService.SchedulerPressure scheduler) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", scheduler.name());
        details.put("queueSize", scheduler.queueSize());
        details.put("activeThreads", scheduler.activeThreads());
        details.put("poolSize", scheduler.poolSize());
        details.put("utilizationRatio", scheduler.utilizationRatio());
        details.put("queueDelta", scheduler.queueDelta());
        details.put("risingFast", scheduler.risingFast());
        details.put("sustained", scheduler.sustained());
        details.put("degraded", scheduler.degraded());
        return details;
    }

    private Map<String, Object> memoryPressure(PjbRuntimePressureService.MemoryPressure memory) {
        if (memory == null) {
            return Map.of();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("heapUsedMiB", memory.heapUsedMiB());
        details.put("heapMaxMiB", memory.heapMaxMiB());
        details.put("heapUsageRatio", memory.heapUsageRatio());
        details.put("heapDeltaMiB", memory.heapDeltaMiB());
        details.put("heapRisingFast", memory.heapRisingFast());
        details.put("heapSustained", memory.heapSustained());
        details.put("metaspaceUsedMiB", memory.metaspaceUsedMiB());
        details.put("metaspaceMaxMiB", memory.metaspaceMaxMiB());
        details.put("metaspaceUsageRatio", memory.metaspaceUsageRatio());
        details.put("directBufferUsedMiB", memory.directBufferUsedMiB());
        details.put("directBufferDeltaMiB", memory.directBufferDeltaMiB());
        details.put("directBufferUsageRatio", memory.directBufferUsageRatio());
        details.put("degraded", memory.degraded());
        details.put("criticalRunaway", memory.criticalRunaway());
        return details;
    }

    private Map<String, Object> gcPressure(PjbRuntimePressureService.GcPressure gc) {
        if (gc == null) {
            return Map.of();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("totalCollections", gc.totalCollections());
        details.put("totalCollectionTimeMillis", gc.totalCollectionTimeMillis());
        details.put("collectionCountDelta", gc.collectionCountDelta());
        details.put("collectionTimeDeltaMillis", gc.collectionTimeDeltaMillis());
        details.put("pauseRatio", gc.pauseRatio());
        details.put("averagePauseMillis", gc.averagePauseMillis());
        details.put("risingFast", gc.risingFast());
        details.put("sustained", gc.sustained());
        details.put("degraded", gc.degraded());
        details.put("critical", gc.critical());
        return details;
    }

    private Map<String, Object> livePressure(PjbLivePressureService.Snapshot live) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("totalSubscribers", live.totalSubscribers());
        details.put("activeTopics", live.activeTopics());
        details.put("subscriberDelta", live.subscriberDelta());
        details.put("risingFast", live.risingFast());
        details.put("sustained", live.sustained());
        details.put("degraded", live.degraded());
        details.put("criticalSurge", live.criticalSurge());
        return details;
    }

    private Map<String, Object> kafkaPressure(PjbKafkaPressureService.Snapshot kafka) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("available", kafka.available());
        details.put("defaultTopic", kafka.defaultTopic());
        details.put("bufferAvailableRatio", kafka.bufferAvailableRatio());
        details.put("requestsInFlight", kafka.requestsInFlight());
        details.put("recordQueueTimeAvg", kafka.recordQueueTimeAvg());
        details.put("recordErrorRate", kafka.recordErrorRate());
        details.put("degraded", kafka.degraded());
        details.put("critical", kafka.critical());
        return details;
    }

    private Map<String, Object> transactionPressure(PjbTransactionPressureView transactions) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("activeTransactions", transactions.activeTransactions());
        details.put("maxObservedConcurrency", transactions.maxObservedConcurrency());
        details.put("startedTransactions", transactions.startedTransactions());
        details.put("completedTransactions", transactions.completedTransactions());
        details.put("failedTransactions", transactions.failedTransactions());
        details.put("longRunningTransactions", transactions.longRunningTransactions());
        details.put("budgetViolations", transactions.budgetViolations());
        details.put("criticalBudgetViolations", transactions.criticalBudgetViolations());
        details.put("longRunningPressure", transactions.longRunningPressure());
        details.put("activePressure", transactions.activePressure());
        details.put("budgetPressure", transactions.budgetPressure());
        details.put("hotOperations", transactions.operations().stream().limit(8).collect(Collectors.toList()));
        return details;
    }

    private List<Map<String, Object>> alerts(List<PjbRuntimePressureService.PressureAlert> alerts) {
        return alerts.stream().limit(8).map(alert -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("severity", alert.severity());
            details.put("source", alert.source());
            details.put("code", alert.code());
            details.put("message", alert.message());
            return details;
        }).collect(Collectors.toList());
    }
}
