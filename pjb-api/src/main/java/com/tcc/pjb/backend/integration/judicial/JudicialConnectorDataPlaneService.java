package com.tcc.pjb.backend.integration.judicial;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorDataPlaneService {

    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorOperationalProfileService operationalProfileService;
    private final JudicialConnectorTelemetryService telemetryService;

    public JudicialConnectorDataPlaneService(JudicialIntegrationProperties integrationProperties,
                                             JudicialConnectorOperationalProfileService operationalProfileService,
                                             JudicialConnectorTelemetryService telemetryService) {
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
    }

    public JudicialConnectorDataPlaneReport nationalReport(Duration horizon) {
        return buildReport(null, horizon);
    }

    public JudicialConnectorDataPlaneReport tribunalReport(String tribunalCodigo,
                                                           Duration horizon) {
        return buildReport(normalizeCode(tribunalCodigo), horizon);
    }

    private JudicialConnectorDataPlaneReport buildReport(String tribunalCodigo,
                                                         Duration horizon) {
        Duration effectiveHorizon = horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon;
        JudicialConnectorTelemetryService.ConnectorTelemetryHealthReport health = telemetryService.buildHealthReport(effectiveHorizon);
        Map<JudicialSystem, JudicialConnectorTelemetryService.ConnectorSystemHealth> healthIndex = new LinkedHashMap<>();
        if (health.systems() != null) {
            health.systems().forEach(item -> healthIndex.put(item.system(), item));
        }
        List<JudicialConnectorDataPlaneSystemReport> systems = Arrays.stream(JudicialSystem.values())
                .map(system -> buildSystemReport(system, tribunalCodigo, healthIndex.get(system), health.horizonStart()))
                .sorted(Comparator
                        .comparing(JudicialConnectorDataPlaneSystemReport::submissionReady).reversed()
                        .thenComparing(JudicialConnectorDataPlaneSystemReport::successRate).reversed()
                        .thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"))
                .toList();
        List<String> readySystems = systems.stream()
                .filter(JudicialConnectorDataPlaneSystemReport::submissionReady)
                .map(item -> item.system() != null ? item.system().name() : JudicialSystem.OUTRO.name())
                .toList();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (health.alerts() != null) {
            alerts.addAll(health.alerts());
        }
        systems.forEach(item -> {
            if (item.warnings() != null) {
                alerts.addAll(item.warnings());
            }
            if (item.blockers() != null && !item.blockers().isEmpty()) {
                alerts.add(item.system().name() + ": " + String.join(" | ", item.blockers()));
            }
        });
        if (tribunalCodigo != null && readySystems.isEmpty()) {
            alerts.add("DATA_PLANE_NO_SUBMISSION_READY_CONNECTOR_FOR_TRIBUNAL");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("telemetry", JudicialMapSupport.compact(
                "generatedAt", health.generatedAt() != null ? health.generatedAt().toString() : null,
                "horizonStart", health.horizonStart() != null ? health.horizonStart().toString() : null,
                "totalSystems", health.totalSystems(),
                "totalEvents", health.totalEvents()
        ));
        metadata.put("dataPlaneMode", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        metadata.put("declaredSystems", Arrays.stream(JudicialSystem.values()).map(Enum::name).sorted().toList());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorDataPlaneReport(
                Instant.now(),
                tribunalCodigo,
                health.horizonStart(),
                health.totalEvents(),
                readySystems,
                systems,
                List.copyOf(alerts),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorDataPlaneSystemReport buildSystemReport(JudicialSystem system,
                                                                     String tribunalCodigo,
                                                                     JudicialConnectorTelemetryService.ConnectorSystemHealth health,
                                                                     Instant horizonStart) {
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        String effectiveTribunal = firstNonBlank(tribunalCodigo, defaultTribunal(cfg));
        ProtocolSubmissionRequest probe = new ProtocolSubmissionRequest(
                "DATA-" + (system != null ? system.name() : JudicialSystem.OUTRO.name()) + '-' + firstNonBlank(effectiveTribunal, "DEFAULT"),
                null,
                "Data Plane Probe",
                effectiveTribunal,
                null,
                null,
                null,
                null,
                null,
                "{}",
                null,
                null,
                null,
                true,
                Map.of("probe", true, "plane", "data")
        );
        JudicialConnectorOperationalProfileReport profile = operationalProfileService.analyze(system, probe);
        long totalEvents = health != null ? health.totalEvents() : 0L;
        long accepted = health != null ? health.acceptedSubmissions() : 0L;
        long rejected = health != null ? health.rejectedSubmissions() : 0L;
        long snapshotHits = health != null ? health.snapshotHits() : 0L;
        long eventSyncHits = health != null ? health.eventSyncHits() : 0L;
        double successRate = health != null ? health.successRate() : 1.0d;
        LinkedHashSet<String> blockers = new LinkedHashSet<>(profile.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>(profile.warnings());
        if (accepted + rejected >= 3L && successRate < 0.50d) {
            warnings.add("DATA_PLANE_DEGRADED_SUCCESS_RATE");
        }
        if (profile.readyForTribunalSubmission() && accepted + rejected == 0L) {
            warnings.add("DATA_PLANE_NO_RECENT_SUBMISSION_ACTIVITY");
        }
        boolean syncReady = profile.homologation() != null
                && profile.homologation().syncHomologated()
                && profile.readiness() != null
                && profile.readiness().syncPathResolved()
                && profile.connectorEnabled();
        String executionStatus = resolveExecutionStatus(profile, totalEvents, accepted, rejected, successRate, blockers, warnings, horizonStart, health != null ? health.latestEventAt() : null);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dominantStatus", health != null ? health.dominantStatus() : null);
        metadata.put("latestMessages", health != null ? health.latestMessages() : List.of());
        metadata.put("horizonStart", horizonStart != null ? horizonStart.toString() : null);
        metadata.put("latestEventAt", health != null && health.latestEventAt() != null ? health.latestEventAt().toString() : null);
        metadata.put("baseUrl", profile.metadata().get("baseUrl"));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorDataPlaneSystemReport(
                Instant.now(),
                system,
                effectiveTribunal,
                executionStatus,
                profile.readyForTribunalSubmission(),
                syncReady,
                totalEvents,
                accepted,
                rejected,
                snapshotHits,
                eventSyncHits,
                round4(successRate),
                health != null ? health.latestEventAt() : null,
                profile,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private String resolveExecutionStatus(JudicialConnectorOperationalProfileReport profile,
                                          long totalEvents,
                                          long accepted,
                                          long rejected,
                                          double successRate,
                                          LinkedHashSet<String> blockers,
                                          LinkedHashSet<String> warnings,
                                          Instant horizonStart,
                                          Instant latestEventAt) {
        if (!blockers.isEmpty()) {
            return "BLOCKED";
        }
        if (!profile.readyForTribunalSubmission()) {
            return "NOT_READY";
        }
        if (accepted + rejected >= 3L && successRate < 0.50d) {
            return "DEGRADED";
        }
        if (totalEvents == 0L) {
            return "READY_WITHOUT_ACTIVITY";
        }
        if (latestEventAt != null && horizonStart != null && latestEventAt.isBefore(horizonStart)) {
            warnings.add("DATA_PLANE_ACTIVITY_OUTSIDE_HORIZON");
            return "STALE";
        }
        return warnings.isEmpty() ? "READY" : "READY_WITH_WARNINGS";
    }

    private double round4(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private String defaultTribunal(JudicialIntegrationProperties.Connector cfg) {
        List<String> homologated = normalizeCodes(cfg != null ? cfg.getHomologatedTribunals() : List.of());
        if (!homologated.isEmpty()) {
            return homologated.getFirst();
        }
        List<String> blocked = normalizeCodes(cfg != null ? cfg.getBlockedTribunals() : List.of());
        if (!blocked.isEmpty()) {
            return blocked.getFirst();
        }
        return null;
    }

    private List<String> normalizeCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeCode(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return new ArrayList<>(out);
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
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
