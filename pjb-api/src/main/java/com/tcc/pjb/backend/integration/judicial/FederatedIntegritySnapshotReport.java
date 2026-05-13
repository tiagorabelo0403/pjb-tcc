package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FederatedIntegritySnapshotReport(
        Instant generatedAt,
        String scopeType,
        String scopeValue,
        String sourceKind,
        Instant horizonStart,
        Instant horizonEnd,
        int leafCount,
        String rootHash,
        String previousRootHash,
        String driftStatus,
        List<String> repairCandidates,
        List<FederatedIntegrityLeafReport> leaves,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("scopeType", scopeType);
        out.put("scopeValue", scopeValue);
        out.put("sourceKind", sourceKind);
        out.put("horizonStart", horizonStart != null ? horizonStart.toString() : null);
        out.put("horizonEnd", horizonEnd != null ? horizonEnd.toString() : null);
        out.put("leafCount", leafCount);
        out.put("rootHash", rootHash);
        out.put("previousRootHash", previousRootHash);
        out.put("driftStatus", driftStatus);
        out.put("repairCandidates", repairCandidates == null ? List.of() : repairCandidates);
        out.put("leaves", leaves == null ? List.of() : leaves.stream().map(FederatedIntegrityLeafReport::toMap).toList());
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(out);
    }
}
