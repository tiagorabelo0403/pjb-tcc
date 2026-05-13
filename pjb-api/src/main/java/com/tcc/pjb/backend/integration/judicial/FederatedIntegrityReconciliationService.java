package com.tcc.pjb.backend.integration.judicial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.judicial.FederatedIntegritySnapshot;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorTelemetry;
import com.tcc.pjb.backend.model.repository.FederatedIntegritySnapshotRepository;
import com.tcc.pjb.backend.model.repository.JudicialConnectorTelemetryRepository;
import java.nio.charset.StandardCharsets;
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
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FederatedIntegrityReconciliationService {

    private final JudicialConnectorTelemetryRepository telemetryRepository;
    private final FederatedIntegritySnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public FederatedIntegrityReconciliationService(JudicialConnectorTelemetryRepository telemetryRepository,
                                                   FederatedIntegritySnapshotRepository snapshotRepository,
                                                   ObjectMapper objectMapper) {
        this.telemetryRepository = Objects.requireNonNull(telemetryRepository);
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public FederatedIntegritySnapshotReport captureNational(Duration horizon) {
        return capture("NATIONAL", null, horizon);
    }

    @Transactional
    public FederatedIntegritySnapshotReport captureTribunal(String tribunalCodigo, Duration horizon) {
        return capture("TRIBUNAL", normalizeTribunal(tribunalCodigo), horizon);
    }

    private FederatedIntegritySnapshotReport capture(String scopeType, String scopeValue, Duration horizon) {
        Duration effective = horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon;
        Instant horizonEnd = Instant.now();
        Instant horizonStart = horizonEnd.minus(effective);
        List<JudicialConnectorTelemetry> events = telemetryRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(horizonStart);
        if (scopeValue != null) {
            events = events.stream().filter(item -> scopeValue.equals(normalizeTribunal(item.getTribunalCodigo()))).toList();
        }
        Map<String, List<JudicialConnectorTelemetry>> grouped = events.stream()
                .filter(item -> item.getConnectorSystem() != null)
                .collect(Collectors.groupingBy(item -> item.getConnectorSystem().name(), LinkedHashMap::new, Collectors.toList()));
        FederatedIntegritySnapshot previous = snapshotRepository.findTop20ByScopeTypeAndScopeValueAndSourceKindOrderByCreatedAtDesc(scopeType, scopeValue, "JUDICIAL_TELEMETRY")
                .stream()
                .findFirst()
                .orElse(null);
        Map<String, Object> previousPayload = previous == null ? Map.of() : parsePayload(previous.getPayloadJson());
        Map<String, String> previousLeaves = extractPreviousLeaves(previousPayload);
        ArrayList<FederatedIntegrityLeafReport> leaves = new ArrayList<>();
        LinkedHashSet<String> repairCandidates = new LinkedHashSet<>();
        for (Map.Entry<String, List<JudicialConnectorTelemetry>> entry : grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            String key = entry.getKey();
            List<JudicialConnectorTelemetry> bucket = entry.getValue();
            bucket.sort(Comparator.comparing(JudicialConnectorTelemetry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(item -> Objects.toString(item.getEventType(), "")));
            String leafHash = computeLeafHash(key, bucket);
            boolean changed = !Objects.equals(previousLeaves.get(key), leafHash);
            if (changed && previous != null) {
                repairCandidates.add(key);
            }
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("latestEventAt", bucket.stream().map(JudicialConnectorTelemetry::getCreatedAt).filter(Objects::nonNull).max(Instant::compareTo).orElse(null));
            metadata.put("tribunais", bucket.stream().map(JudicialConnectorTelemetry::getTribunalCodigo).filter(Objects::nonNull).map(this::normalizeTribunal).distinct().toList());
            metadata.put("acceptedCount", bucket.stream().filter(item -> Boolean.TRUE.equals(item.getAccepted())).count());
            metadata.put("rejectedCount", bucket.stream().filter(item -> Boolean.FALSE.equals(item.getAccepted())).count());
            metadata.entrySet().removeIf(it -> it.getValue() == null);
            leaves.add(new FederatedIntegrityLeafReport(key, leafHash, bucket.size(), changed, Map.copyOf(metadata)));
        }
        String rootHash = computeRootHash(leaves);
        String previousRoot = previous != null ? previous.getRootHash() : null;
        String driftStatus = previous == null ? "INITIAL" : Objects.equals(previousRoot, rootHash) ? "CONSISTENT" : leaves.isEmpty() ? "EMPTY" : "DRIFTED";
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("leaves", leaves.stream().map(FederatedIntegrityLeafReport::toMap).toList());
        payload.put("repairCandidates", List.copyOf(repairCandidates));
        payload.put("eventCount", events.size());
        payload.put("scopeType", scopeType);
        payload.put("scopeValue", scopeValue);
        payload.put("sourceKind", "JUDICIAL_TELEMETRY");
        payload.put("generatedAt", Instant.now().toString());
        FederatedIntegritySnapshot snapshot = FederatedIntegritySnapshot.builder()
                .id(UUID.randomUUID())
                .scopeType(scopeType)
                .scopeValue(scopeValue)
                .sourceKind("JUDICIAL_TELEMETRY")
                .horizonStart(horizonStart)
                .horizonEnd(horizonEnd)
                .leafCount(leaves.size())
                .rootHash(rootHash)
                .previousRootHash(previousRoot)
                .driftStatus(driftStatus)
                .payloadJson(serialize(payload))
                .build();
        snapshotRepository.save(snapshot);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("eventCount", events.size());
        metadata.put("scopeType", scopeType);
        metadata.put("scopeValue", scopeValue);
        metadata.put("sourceKind", "JUDICIAL_TELEMETRY");
        metadata.put("previousSnapshotAt", previous != null ? previous.getCreatedAt() : null);
        metadata.put("repairCandidateCount", repairCandidates.size());
        metadata.entrySet().removeIf(it -> it.getValue() == null);
        return new FederatedIntegritySnapshotReport(
                Instant.now(),
                scopeType,
                scopeValue,
                "JUDICIAL_TELEMETRY",
                horizonStart,
                horizonEnd,
                leaves.size(),
                rootHash,
                previousRoot,
                driftStatus,
                List.copyOf(repairCandidates),
                List.copyOf(leaves),
                Map.copyOf(metadata)
        );
    }

    private String computeLeafHash(String key, List<JudicialConnectorTelemetry> bucket) {
        StringBuilder builder = new StringBuilder(key).append('|');
        for (JudicialConnectorTelemetry item : bucket) {
            builder.append(Objects.toString(item.getEventType(), "")).append('|')
                    .append(Objects.toString(item.getStatus(), "")).append('|')
                    .append(Objects.toString(item.getProtocolReference(), "")).append('|')
                    .append(Objects.toString(item.getMessage(), "")).append('|')
                    .append(item.getCreatedAt() != null ? item.getCreatedAt().toString() : "").append('|')
                    .append(Objects.toString(item.getPayloadJson(), "")).append('\n');
        }
        return Hashes.sha256Hex(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String computeRootHash(List<FederatedIntegrityLeafReport> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return Hashes.sha256Hex("EMPTY_INTEGRITY_SCOPE".getBytes(StandardCharsets.UTF_8));
        }
        String material = leaves.stream().sorted(Comparator.comparing(FederatedIntegrityLeafReport::key)).map(item -> item.key() + ':' + item.hash()).collect(Collectors.joining("|"));
        return Hashes.sha256Hex(material.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractPreviousLeaves(Map<String, Object> payload) {
        Object rawLeaves = payload.get("leaves");
        if (!(rawLeaves instanceof List<?> list) || list.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object key = map.get("key");
                Object hash = map.get("hash");
                if (key != null && hash != null) {
                    out.put(String.valueOf(key), String.valueOf(hash));
                }
            }
        }
        return Map.copyOf(out);
    }

    private Map<String, Object> parsePayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            return String.valueOf(payload);
        }
    }

    private String normalizeTribunal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
