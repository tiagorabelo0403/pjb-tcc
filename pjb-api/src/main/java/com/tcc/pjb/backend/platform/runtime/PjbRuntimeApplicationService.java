package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.runtime.domain.PjbReadAfterWritePolicyView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbReadAfterWriteRequestView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeDrainView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeHealthView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeMemoryBudgetView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimePressureView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeSizingView;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbRuntimeApplicationService {

    private final PjbRuntimePressureService pressureService;
    private final PjbRuntimeDrainService drainService;
    private final PjbRuntimeSizingPolicy.Footprint footprint;
    private final PjbRuntimeAccelerationProperties accelerationProperties;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;
    private final AuditLedgerService auditLedgerService;

    public PjbRuntimeApplicationService(PjbRuntimePressureService pressureService,
                                        PjbRuntimeDrainService drainService,
                                        PjbRuntimeSizingPolicy.Footprint footprint,
                                        PjbRuntimeAccelerationProperties accelerationProperties,
                                        ReadAfterWriteConsistencyPolicy rawPolicy,
                                        AuditLedgerService auditLedgerService) {
        this.pressureService = Objects.requireNonNull(pressureService);
        this.drainService = Objects.requireNonNull(drainService);
        this.footprint = Objects.requireNonNull(footprint);
        this.accelerationProperties = Objects.requireNonNull(accelerationProperties);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbRuntimeSizingView sizing() {
        return new PjbRuntimeSizingView(
                footprint.availableProcessors(),
                footprint.maxMemoryMiB(),
                normalizeRole(accelerationProperties.getComponentRole()));
    }

    @Transactional(readOnly = true)
    public PjbRuntimeMemoryBudgetView memoryBudget() {
        long baseline = footprint.maxMemoryMiB();
        long heap = baseline;
        long direct = clamp(Math.round(baseline * 0.18d), 128L, 1024L);
        long metaspace = clamp(Math.round(baseline * 0.10d), 128L, 768L);
        long codeCache = clamp(Math.round(baseline * 0.05d), 64L, 256L);
        long nativeReserve = clamp(Math.round(baseline * 0.20d), 128L, 1536L);
        long plannedEnvelope = heap + direct + metaspace + codeCache + nativeReserve;
        return new PjbRuntimeMemoryBudgetView(heap, direct, metaspace, codeCache, nativeReserve, plannedEnvelope, baseline);
    }

    @Transactional(readOnly = true)
    public PjbRuntimePressureView pressure() {
        PjbRuntimePressureService.Snapshot snapshot = pressureService.snapshot();
        auditLedgerService.appendSafely(
                "RUNTIME_PRESSURE_QUERY",
                "RUNTIME",
                snapshot.componentRole(),
                null,
                "score=" + snapshot.pressureScore() + " headroom=" + snapshot.headroomScore());
        return new PjbRuntimePressureView(
                snapshot.ready(),
                snapshot.warmingUp(),
                snapshot.pressureScore(),
                snapshot.headroomScore(),
                snapshot.componentRole(),
                snapshot.availableProcessors(),
                snapshot.maxMemoryMiB(),
                snapshot.generatedAt(),
                snapshot.uptimeMillis(),
                snapshot.overloadedExecutorNames(),
                snapshot.degradedDatasourceNames(),
                snapshot.criticalOverloadedExecutorNames(),
                snapshot.criticalMemoryRunaway(),
                snapshot.criticalGcPressure(),
                trendLabel(snapshot.trend()),
                snapshot.alerts().size());
    }

    @Transactional(readOnly = true)
    public PjbRuntimeDrainView drain() {
        PjbRuntimeDrainService.Snapshot snapshot = drainService.snapshot();
        return new PjbRuntimeDrainView(
                snapshot.draining(),
                snapshot.readyForTraffic(),
                snapshot.drainingSince(),
                snapshot.drainAgeMillis(),
                snapshot.reason(),
                snapshot.drainQuietPeriod().toMillis());
    }

    @Transactional
    public PjbRuntimeDrainView beginDrain(String reason) {
        String normalizedReason = normalizeReason(reason, "manual-drain");
        drainService.beginDrain(normalizedReason);
        PjbRuntimeDrainView view = drain();
        auditLedgerService.appendSafely("RUNTIME_DRAIN_BEGIN", "RUNTIME", normalizedReason, null, "draining=" + view.draining());
        return view;
    }

    @Transactional
    public PjbRuntimeDrainView acceptTraffic(String reason) {
        String normalizedReason = normalizeReason(reason, "manual-accepting");
        drainService.markAccepting(normalizedReason);
        PjbRuntimeDrainView view = drain();
        auditLedgerService.appendSafely("RUNTIME_DRAIN_ACCEPT", "RUNTIME", normalizedReason, null, "ready=" + view.readyForTraffic());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbRuntimeHealthView health() {
        PjbRuntimePressureService.Snapshot pressure = pressureService.snapshot();
        PjbRuntimeDrainService.Snapshot drain = drainService.snapshot();
        String status = drain.draining()
                ? "OUT_OF_SERVICE"
                : pressure.ready() ? "UP" : "DEGRADED";
        String summary = "score=" + pressure.pressureScore() + " draining=" + drain.draining() + " trend=" + trendLabel(pressure.trend());
        return new PjbRuntimeHealthView(status, pressure.ready(), drain.draining(), summary);
    }

    @Transactional(readOnly = true)
    public PjbReadAfterWritePolicyView rawPolicy() {
        return new PjbReadAfterWritePolicyView(rawPolicy.windowMillis(), true, "PRIMARY_STRICT");
    }

    @Transactional(readOnly = true)
    public PjbReadAfterWriteRequestView rawRequest() {
        Long lastWriteAt = rawPolicy.lastWriteAtEpochMillis();
        return new PjbReadAfterWriteRequestView(
                lastWriteAt != null,
                rawPolicy.shouldForcePrimary(),
                lastWriteAt,
                rawPolicy.windowMillis());
    }

    private String trendLabel(PjbRuntimePressureService.PressureTrend trend) {
        if (trend == null) {
            return "steady";
        }
        if (trend.risingFast()) {
            return "rising_fast";
        }
        if (trend.sustained()) {
            return "sustained";
        }
        if (trend.deltaScore() < 0) {
            return "relieving";
        }
        return "steady";
    }

    private String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return "mixed";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeReason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
