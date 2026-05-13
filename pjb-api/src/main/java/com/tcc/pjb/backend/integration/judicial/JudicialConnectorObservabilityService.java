package com.tcc.pjb.backend.integration.judicial;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorObservabilityService {

    private final JudicialConnectorControlPlaneService controlPlaneService;
    private final JudicialConnectorDataPlaneService dataPlaneService;

    public JudicialConnectorObservabilityService(JudicialConnectorControlPlaneService controlPlaneService,
                                                 JudicialConnectorDataPlaneService dataPlaneService) {
        this.controlPlaneService = Objects.requireNonNull(controlPlaneService);
        this.dataPlaneService = Objects.requireNonNull(dataPlaneService);
    }

    public JudicialConnectorObservabilityReport nationalReport(Duration horizon) {
        return buildReport(null, horizon);
    }

    public JudicialConnectorObservabilityReport tribunalReport(String tribunalCodigo, Duration horizon) {
        return buildReport(tribunalCodigo, horizon);
    }

    private JudicialConnectorObservabilityReport buildReport(String tribunalCodigo, Duration horizon) {
        Duration effectiveHorizon = horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon;
        JudicialConnectorControlPlaneReport controlPlane = tribunalCodigo == null
                ? controlPlaneService.nationalReport()
                : controlPlaneService.tribunalReport(tribunalCodigo);
        JudicialConnectorDataPlaneReport dataPlane = tribunalCodigo == null
                ? dataPlaneService.nationalReport(effectiveHorizon)
                : dataPlaneService.tribunalReport(tribunalCodigo, effectiveHorizon);
        Map<JudicialSystem, JudicialConnectorControlPlaneSystemReport> controlIndex = new LinkedHashMap<>();
        if (controlPlane.systems() != null) {
            controlPlane.systems().forEach(item -> controlIndex.put(item.system(), item));
        }
        List<JudicialConnectorObservabilitySystemReport> systems = (dataPlane.systems() == null ? List.<JudicialConnectorDataPlaneSystemReport>of() : dataPlane.systems()).stream()
                .map(item -> buildSystemReport(item, controlIndex.get(item.system())))
                .sorted(Comparator
                        .comparing((JudicialConnectorObservabilitySystemReport item) -> "HEALTHY".equals(item.observabilityStatus())).reversed()
                        .thenComparing(JudicialConnectorObservabilitySystemReport::successRate).reversed()
                        .thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"))
                .toList();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (controlPlane.blockers() != null) {
            alerts.addAll(controlPlane.blockers());
        }
        if (controlPlane.warnings() != null) {
            alerts.addAll(controlPlane.warnings());
        }
        if (dataPlane.alerts() != null) {
            alerts.addAll(dataPlane.alerts());
        }
        systems.forEach(item -> {
            if (item.blockers() != null) {
                alerts.addAll(item.blockers());
            }
            if (item.warnings() != null) {
                alerts.addAll(item.warnings());
            }
        });
        int healthySystems = (int) systems.stream().filter(item -> "HEALTHY".equals(item.observabilityStatus())).count();
        int degradedSystems = (int) systems.stream().filter(item -> "DEGRADED".equals(item.observabilityStatus()) || "NO_ACTIVITY".equals(item.observabilityStatus()) || "STALE".equals(item.observabilityStatus())).count();
        int blockedSystems = (int) systems.stream().filter(item -> "BLOCKED".equals(item.observabilityStatus())).count();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        metadata.put("controlPlane", JudicialMapSupport.compact(
                "registeredConnectorCount", controlPlane.registeredConnectorCount(),
                "enabledConnectorCount", controlPlane.enabledConnectorCount(),
                "operationalConnectorCount", controlPlane.operationalConnectorCount()
        ));
        metadata.put("dataPlane", JudicialMapSupport.compact(
                "totalEvents", dataPlane.totalEvents(),
                "readySystems", dataPlane.readySystems().size()
        ));
        return new JudicialConnectorObservabilityReport(
                Instant.now(),
                tribunalCodigo,
                dataPlane.horizonStart(),
                healthySystems,
                degradedSystems,
                blockedSystems,
                systems,
                new ArrayList<>(alerts),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorObservabilitySystemReport buildSystemReport(JudicialConnectorDataPlaneSystemReport dataPlane,
                                                                         JudicialConnectorControlPlaneSystemReport controlPlane) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (controlPlane != null) {
            if (controlPlane.blockers() != null) {
                blockers.addAll(controlPlane.blockers());
            }
            if (controlPlane.warnings() != null) {
                warnings.addAll(controlPlane.warnings());
            }
        }
        if (dataPlane.blockers() != null) {
            blockers.addAll(dataPlane.blockers());
        }
        if (dataPlane.warnings() != null) {
            warnings.addAll(dataPlane.warnings());
        }
        String status = resolveStatus(dataPlane, blockers, warnings);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("controlStatus", controlPlane != null ? controlPlane.controlStatus() : null);
        metadata.put("executionStatus", dataPlane.executionStatus());
        metadata.put("acceptedSubmissions", dataPlane.acceptedSubmissions());
        metadata.put("rejectedSubmissions", dataPlane.rejectedSubmissions());
        metadata.put("snapshotHits", dataPlane.snapshotHits());
        metadata.put("eventSyncHits", dataPlane.eventSyncHits());
        metadata.put("latestMessages", dataPlane.metadata() != null ? dataPlane.metadata().get("latestMessages") : null);
        metadata.put("dominantStatus", dataPlane.metadata() != null ? dataPlane.metadata().get("dominantStatus") : null);
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorObservabilitySystemReport(
                Instant.now(),
                dataPlane.system(),
                dataPlane.tribunalCodigo(),
                status,
                controlPlane != null && controlPlane.productionReady(),
                controlPlane != null && controlPlane.tribunalReady(),
                dataPlane.submissionReady(),
                dataPlane.syncReady(),
                dataPlane.totalEvents() > 0L,
                dataPlane.totalEvents(),
                dataPlane.successRate(),
                dataPlane.latestEventAt(),
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private String resolveStatus(JudicialConnectorDataPlaneSystemReport dataPlane,
                                 LinkedHashSet<String> blockers,
                                 LinkedHashSet<String> warnings) {
        if (!blockers.isEmpty()) {
            return "BLOCKED";
        }
        if ("DEGRADED".equals(dataPlane.executionStatus())) {
            return "DEGRADED";
        }
        if ("STALE".equals(dataPlane.executionStatus())) {
            return "STALE";
        }
        if ("READY_WITHOUT_ACTIVITY".equals(dataPlane.executionStatus())) {
            return "NO_ACTIVITY";
        }
        if (dataPlane.totalEvents() == 0L && dataPlane.submissionReady()) {
            return "NO_ACTIVITY";
        }
        if (!warnings.isEmpty()) {
            return "HEALTHY_WITH_WARNINGS";
        }
        return "HEALTHY";
    }
}
