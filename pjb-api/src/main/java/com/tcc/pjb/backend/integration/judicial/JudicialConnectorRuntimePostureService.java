package com.tcc.pjb.backend.integration.judicial;

import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorAdminOperation;
import com.tcc.pjb.backend.model.repository.JudicialConnectorAdminOperationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorRuntimePostureService {

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(24);
    private static final Duration STALE_AFTER = Duration.ofMinutes(90);
    private static final Duration QUARANTINE_LOOKBACK = Duration.ofDays(7);
    private static final Duration MAINTENANCE_LOOKBACK = Duration.ofDays(2);

    private final JudicialConnectorControlPlaneService controlPlaneService;
    private final JudicialConnectorObservabilityService observabilityService;
    private final JudicialConnectorAdminOperationRepository adminOperationRepository;

    public JudicialConnectorRuntimePostureService(JudicialConnectorControlPlaneService controlPlaneService,
                                                  JudicialConnectorObservabilityService observabilityService,
                                                  JudicialConnectorAdminOperationRepository adminOperationRepository) {
        this.controlPlaneService = Objects.requireNonNull(controlPlaneService);
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.adminOperationRepository = Objects.requireNonNull(adminOperationRepository);
    }

    public JudicialConnectorRuntimePostureReport nationalReport() {
        return build(null);
    }

    public JudicialConnectorRuntimePostureReport tribunalReport(String tribunalCodigo) {
        return build(normalizeTribunal(tribunalCodigo));
    }

    private JudicialConnectorRuntimePostureReport build(String tribunalCodigo) {
        JudicialConnectorControlPlaneReport controlPlane = tribunalCodigo == null
                ? controlPlaneService.nationalReport()
                : controlPlaneService.tribunalReport(tribunalCodigo);
        JudicialConnectorObservabilityReport observability = tribunalCodigo == null
                ? observabilityService.nationalReport(DEFAULT_WINDOW)
                : observabilityService.tribunalReport(tribunalCodigo, DEFAULT_WINDOW);
        Map<JudicialSystem, JudicialConnectorObservabilitySystemReport> observabilityIndex = new LinkedHashMap<>();
        if (observability.systems() != null) {
            observability.systems().forEach(item -> observabilityIndex.put(item.system(), item));
        }
        List<JudicialConnectorAdminOperation> operations = adminOperationRepository.findTop100ByOrderByCreatedAtDesc();
        ArrayList<JudicialConnectorRuntimePostureSystemReport> systems = new ArrayList<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        for (JudicialConnectorControlPlaneSystemReport systemReport : controlPlane.systems()) {
            JudicialConnectorObservabilitySystemReport observabilityReport = observabilityIndex.get(systemReport.system());
            systems.add(buildSystemReport(systemReport, observabilityReport, tribunalCodigo, operations));
        }
        systems.sort(Comparator.comparing(JudicialConnectorRuntimePostureSystemReport::runtimeStatus).thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"));
        int healthy = 0;
        int degraded = 0;
        int quarantined = 0;
        int blocked = 0;
        for (JudicialConnectorRuntimePostureSystemReport item : systems) {
            switch (item.runtimeStatus()) {
                case "HEALTHY" -> healthy++;
                case "QUARANTINED" -> quarantined++;
                case "BLOCKED" -> blocked++;
                default -> degraded++;
            }
            if (item.backpressureRecommended()) {
                alerts.add("Backpressure recomendado para " + item.system().name() + ".");
            }
            if (item.readOnlyProjectionRecommended()) {
                alerts.add("Leitura degradada por projeção local recomendada para " + item.system().name() + ".");
            }
            if (item.resyncRecommended()) {
                alerts.add("Reconciliação incremental recomendada para " + item.system().name() + ".");
            }
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("windowHours", DEFAULT_WINDOW.toHours());
        metadata.put("staleAfterMinutes", STALE_AFTER.toMinutes());
        metadata.put("quarantineLookbackHours", QUARANTINE_LOOKBACK.toHours());
        metadata.put("maintenanceLookbackHours", MAINTENANCE_LOOKBACK.toHours());
        metadata.put("controlPlaneStatus", controlPlane.metadata());
        metadata.put("observabilityStatus", observability.metadata());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorRuntimePostureReport(
                Instant.now(),
                tribunalCodigo,
                systems.size(),
                healthy,
                degraded,
                quarantined,
                blocked,
                List.copyOf(systems),
                List.copyOf(alerts),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorRuntimePostureSystemReport buildSystemReport(JudicialConnectorControlPlaneSystemReport control,
                                                                          JudicialConnectorObservabilitySystemReport observability,
                                                                          String tribunalCodigo,
                                                                          List<JudicialConnectorAdminOperation> operations) {
        Instant now = Instant.now();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (control.blockers() != null) {
            blockers.addAll(control.blockers());
        }
        if (control.warnings() != null) {
            warnings.addAll(control.warnings());
        }
        if (observability != null && observability.blockers() != null) {
            blockers.addAll(observability.blockers());
        }
        if (observability != null && observability.warnings() != null) {
            warnings.addAll(observability.warnings());
        }
        boolean quarantine = hasRecentOperation(operations, control.system(), tribunalCodigo, "QUARANTINE", QUARANTINE_LOOKBACK, now);
        boolean maintenance = hasRecentOperation(operations, control.system(), tribunalCodigo, "MAINTENANCE", MAINTENANCE_LOOKBACK, now)
                || hasRecentOperation(operations, control.system(), tribunalCodigo, "MAINTENANCE_MODE", MAINTENANCE_LOOKBACK, now);
        Instant latestEventAt = observability != null ? observability.latestEventAt() : null;
        long stalenessSeconds = latestEventAt == null ? Long.MAX_VALUE : Math.max(0L, Duration.between(latestEventAt, now).getSeconds());
        boolean stale = latestEventAt == null || stalenessSeconds >= STALE_AFTER.getSeconds();
        boolean blocked = !blockers.isEmpty() || "BLOCKED".equals(control.controlStatus());
        boolean degradedByObservability = observability != null && !List.of("HEALTHY", "HEALTHY_WITH_WARNINGS").contains(observability.observabilityStatus());
        boolean backpressureRecommended = quarantine || maintenance || (observability != null && observability.successRate() < 0.80d && observability.totalEvents() >= 10L);
        boolean readOnlyProjectionRecommended = quarantine || maintenance || stale || degradedByObservability;
        boolean resyncRecommended = stale || (observability != null && observability.totalEvents() == 0L && control.tribunalReady());
        String runtimeStatus;
        if (blocked) {
            runtimeStatus = "BLOCKED";
        } else if (quarantine) {
            runtimeStatus = "QUARANTINED";
        } else if (stale && control.tribunalReady()) {
            runtimeStatus = "STALE_READ_ONLY";
        } else if (maintenance || degradedByObservability || !control.tribunalReady()) {
            runtimeStatus = "DEGRADED";
        } else {
            runtimeStatus = "HEALTHY";
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("controlStatus", control.controlStatus());
        metadata.put("observabilityStatus", observability != null ? observability.observabilityStatus() : null);
        metadata.put("tribunalReady", control.tribunalReady());
        metadata.put("productionReady", control.productionReady());
        metadata.put("submissionReady", observability != null && observability.submissionReady());
        metadata.put("syncReady", observability != null && observability.syncReady());
        metadata.put("successRate", observability != null ? observability.successRate() : null);
        metadata.put("totalEvents", observability != null ? observability.totalEvents() : 0L);
        metadata.put("recommendedLane", readOnlyProjectionRecommended ? "READ_LOCAL_PROJECTION" : "LIVE_FEDERATED_ALLOWED");
        metadata.put("recommendedAction", resyncRecommended ? "TRIGGER_INCREMENTAL_RECONCILIATION" : backpressureRecommended ? "LIMIT_WRITE_PRESSURE" : "KEEP_STEADY");
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorRuntimePostureSystemReport(
                now,
                control.system(),
                firstNonBlank(tribunalCodigo, control.tribunalCodigo()),
                runtimeStatus,
                quarantine,
                maintenance,
                readOnlyProjectionRecommended,
                backpressureRecommended,
                resyncRecommended,
                latestEventAt,
                stalenessSeconds,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private boolean hasRecentOperation(List<JudicialConnectorAdminOperation> operations,
                                       JudicialSystem system,
                                       String tribunalCodigo,
                                       String token,
                                       Duration lookback,
                                       Instant now) {
        if (operations == null || operations.isEmpty()) {
            return false;
        }
        Instant cutoff = now.minus(lookback);
        for (JudicialConnectorAdminOperation operation : operations) {
            if (operation == null || operation.getCreatedAt() == null || operation.getCreatedAt().isBefore(cutoff)) {
                continue;
            }
            if (system != null && operation.getConnectorSystem() != system) {
                continue;
            }
            if (tribunalCodigo != null && operation.getTribunalCodigo() != null && !normalizeTribunal(tribunalCodigo).equals(normalizeTribunal(operation.getTribunalCodigo()))) {
                continue;
            }
            String type = normalizeToken(operation.getOperationType());
            String status = normalizeToken(operation.getOutcomeStatus());
            if ((type.contains(token) || status.contains(token)) && !status.contains("FAILED")) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTribunal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
