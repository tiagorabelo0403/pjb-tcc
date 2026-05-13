package com.tcc.pjb.backend.integration.judicial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorTelemetry;
import com.tcc.pjb.backend.model.repository.JudicialConnectorTelemetryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorTelemetryService {

    public record ConnectorSystemHealth(
            JudicialSystem system,
            long totalEvents,
            long acceptedSubmissions,
            long rejectedSubmissions,
            long snapshotHits,
            long eventSyncHits,
            double successRate,
            String dominantStatus,
            Instant latestEventAt,
            List<String> latestMessages
    ) {
    }

    public record ConnectorTelemetryHealthReport(
            Instant generatedAt,
            Instant horizonStart,
            int totalSystems,
            long totalEvents,
            List<ConnectorSystemHealth> systems,
            List<String> alerts
    ) {
    }

    private final JudicialConnectorTelemetryRepository repository;
    private final ObjectMapper objectMapper;

    public JudicialConnectorTelemetryService(JudicialConnectorTelemetryRepository repository,
                                             ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public void recordSubmissionRequest(Processo processo,
                                        ProtocolSubmissionRequest request,
                                        ProceduralSubmissionBlueprintReport blueprint,
                                        ProceduralConnectorExecutionReport execution) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request != null ? request.requestId() : null);
        payload.put("title", request != null ? request.title() : null);
        payload.put("dryRun", request != null && request.dryRun());
        payload.put("executionMode", execution != null ? execution.executionMode() : null);
        payload.put("submissionLane", execution != null ? execution.submissionLane() : null);
        payload.put("tribunalTargetKey", execution != null ? execution.tribunalTargetKey() : null);
        payload.put("blueprintStatus", blueprint != null ? blueprint.blueprintStatus() : null);
        payload.put("connectorChecklist", execution != null ? execution.executionChecklist() : null);
        persist(buildEntry(
                processo,
                request != null ? request.tribunalCodigo() : null,
                request != null ? request.unidadeJudiciariaCodigo() : null,
                execution != null ? execution.judicialSystem() : blueprint != null ? blueprint.judicialSystem() : null,
                "SUBMISSION_REQUEST",
                "DISPATCHED",
                null,
                null,
                payload
        ));
    }

    @Transactional
    public void recordSubmissionResult(Processo processo,
                                       ProtocolSubmissionRequest request,
                                       ProtocolSubmissionResult result) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request != null ? request.requestId() : null);
        payload.put("tribunalCodigo", request != null ? request.tribunalCodigo() : null);
        payload.put("unidadeJudiciariaCodigo", request != null ? request.unidadeJudiciariaCodigo() : null);
        payload.put("raw", result != null ? result.raw() : Map.of());
        persist(buildEntry(
                processo,
                request != null ? request.tribunalCodigo() : null,
                request != null ? request.unidadeJudiciariaCodigo() : null,
                result != null ? result.system() : null,
                "SUBMISSION_RESULT",
                result != null ? result.status() : "UNKNOWN",
                result != null ? result.accepted() : null,
                result != null ? result.protocolReference() : null,
                mergeMessagePayload(result != null ? result.message() : null, payload)
        ));
    }

    @Transactional
    public void recordSnapshotResult(JudicialSystem system,
                                     String numeroUnificado,
                                     ExternalProcessSnapshot snapshot) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshotPresent", snapshot != null);
        payload.put("snapshot", snapshot != null ? snapshot.raw() : Map.of());
        persist(buildEntry(
                null,
                null,
                null,
                system,
                "SNAPSHOT_SYNC",
                snapshot != null ? "SNAPSHOT_FOUND" : "SNAPSHOT_EMPTY",
                snapshot != null,
                null,
                withNumero(numeroUnificado, payload)
        ));
    }

    @Transactional
    public void recordEventsResult(JudicialSystem system,
                                   String numeroUnificado,
                                   Instant since,
                                   int totalEvents) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("since", since != null ? since.toString() : null);
        payload.put("totalEvents", totalEvents);
        persist(buildEntry(
                null,
                null,
                null,
                system,
                "EVENT_SYNC",
                totalEvents > 0 ? "EVENTS_FOUND" : "EVENTS_EMPTY",
                totalEvents > 0,
                null,
                withNumero(numeroUnificado, payload)
        ));
    }

    @Transactional(readOnly = true)
    public ConnectorTelemetryHealthReport buildHealthReport(Duration horizon) {
        Duration effectiveHorizon = horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon;
        Instant horizonStart = Instant.now().minus(effectiveHorizon);
        List<JudicialConnectorTelemetry> entries = repository.findAllByCreatedAtAfterOrderByCreatedAtDesc(horizonStart);
        LinkedHashMap<JudicialSystem, List<JudicialConnectorTelemetry>> grouped = new LinkedHashMap<>();
        for (JudicialConnectorTelemetry entry : entries) {
            if (entry.getConnectorSystem() == null) {
                continue;
            }
            grouped.computeIfAbsent(entry.getConnectorSystem(), ignored -> new ArrayList<>()).add(entry);
        }
        List<ConnectorSystemHealth> systems = new ArrayList<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        for (Map.Entry<JudicialSystem, List<JudicialConnectorTelemetry>> bucket : grouped.entrySet()) {
            JudicialSystem system = bucket.getKey();
            List<JudicialConnectorTelemetry> events = bucket.getValue();
            long accepted = events.stream()
                    .filter(item -> "SUBMISSION_RESULT".equals(item.getEventType()))
                    .filter(item -> Boolean.TRUE.equals(item.getAccepted()))
                    .count();
            long rejected = events.stream()
                    .filter(item -> "SUBMISSION_RESULT".equals(item.getEventType()))
                    .filter(item -> Boolean.FALSE.equals(item.getAccepted()))
                    .count();
            long snapshotHits = events.stream()
                    .filter(item -> "SNAPSHOT_SYNC".equals(item.getEventType()))
                    .filter(item -> Boolean.TRUE.equals(item.getAccepted()))
                    .count();
            long eventHits = events.stream()
                    .filter(item -> "EVENT_SYNC".equals(item.getEventType()))
                    .filter(item -> Boolean.TRUE.equals(item.getAccepted()))
                    .count();
            long submissionTotal = accepted + rejected;
            double successRate = submissionTotal == 0L ? 1.0d : (double) accepted / (double) submissionTotal;
            String dominantStatus = dominantStatus(events);
            Instant latestEventAt = events.stream().map(JudicialConnectorTelemetry::getCreatedAt).filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
            List<String> latestMessages = events.stream()
                    .map(JudicialConnectorTelemetry::getMessage)
                    .filter(Objects::nonNull)
                    .filter(message -> !message.isBlank())
                    .limit(5)
                    .toList();
            systems.add(new ConnectorSystemHealth(system, events.size(), accepted, rejected, snapshotHits, eventHits, round4(successRate), dominantStatus, latestEventAt, latestMessages));
            if (submissionTotal >= 3L && successRate < 0.50d) {
                alerts.add("Baixa taxa de sucesso de protocolo em " + system.name() + ": " + Math.round(successRate * 100.0d) + "% no horizonte observado.");
            }
            if (submissionTotal > 0L && snapshotHits == 0L) {
                alerts.add("Protocolos enviados sem retorno de snapshot em " + system.name() + ".");
            }
        }
        return new ConnectorTelemetryHealthReport(Instant.now(), horizonStart, systems.size(), entries.size(), List.copyOf(systems), List.copyOf(alerts));
    }

    private JudicialConnectorTelemetry buildEntry(Processo processo,
                                                  String tribunalCodigo,
                                                  String unidadeJudiciariaCodigo,
                                                  JudicialSystem system,
                                                  String eventType,
                                                  String status,
                                                  Boolean accepted,
                                                  String protocolReference,
                                                  Map<String, Object> payload) {
        JudicialConnectorTelemetry entry = new JudicialConnectorTelemetry();
        entry.setProcessoId(processo != null ? processo.getId() : null);
        entry.setNumeroUnificado(processo != null ? firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()) : extractNumero(payload));
        entry.setTribunalCodigo(firstNonBlank(tribunalCodigo, processo != null ? processo.getTribunalCodigoRoteado() : null));
        entry.setUnidadeJudiciariaCodigo(firstNonBlank(unidadeJudiciariaCodigo, processo != null ? processo.getUnidadeJudiciariaCodigo() : null));
        entry.setConnectorSystem(system);
        entry.setEventType(normalizeToken(eventType));
        entry.setStatus(normalizeToken(status));
        entry.setAccepted(accepted);
        entry.setProtocolReference(protocolReference);
        entry.setMessage(truncate(extractMessage(payload), 1000));
        entry.setPayloadJson(serialize(payload));
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private Map<String, Object> mergeMessagePayload(String message, Map<String, Object> payload) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (payload != null) {
            merged.putAll(payload);
        }
        merged.put("message", message);
        return merged;
    }

    private Map<String, Object> withNumero(String numeroUnificado, Map<String, Object> payload) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (payload != null) {
            merged.putAll(payload);
        }
        merged.put("numeroUnificado", numeroUnificado);
        return merged;
    }

    private void persist(JudicialConnectorTelemetry entry) {
        repository.save(entry);
    }

    private String serialize(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return truncate(objectMapper.writeValueAsString(payload), 32000);
        } catch (JsonProcessingException ignored) {
            return truncate(String.valueOf(payload), 32000);
        }
    }

    private String extractMessage(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object direct = payload.get("message");
        if (direct != null && !String.valueOf(direct).isBlank()) {
            return String.valueOf(direct).trim();
        }
        Object totalEvents = payload.get("totalEvents");
        if (totalEvents != null) {
            return "Eventos sincronizados: " + totalEvents;
        }
        Object snapshotPresent = payload.get("snapshotPresent");
        if (snapshotPresent != null) {
            return Boolean.TRUE.equals(snapshotPresent) ? "Snapshot externo localizado." : "Snapshot externo não localizado.";
        }
        return null;
    }

    private String extractNumero(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object numero = payload.get("numeroUnificado");
        return numero == null ? null : String.valueOf(numero);
    }

    private String dominantStatus(List<JudicialConnectorTelemetry> entries) {
        LinkedHashMap<String, Integer> frequency = new LinkedHashMap<>();
        for (JudicialConnectorTelemetry entry : entries) {
            String status = normalizeToken(entry.getStatus());
            if (status == null) {
                continue;
            }
            frequency.merge(status, 1, Integer::sum);
        }
        return frequency.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private double round4(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isBlank() ? null : normalized;
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

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 1));
    }
}
