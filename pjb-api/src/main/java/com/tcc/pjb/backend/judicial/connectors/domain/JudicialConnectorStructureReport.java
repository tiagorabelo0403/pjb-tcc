package com.tcc.pjb.backend.judicial.connectors.domain;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorStructureReport(
        Instant generatedAt,
        String consolidatedPackageRoot,
        List<String> legacyPackageRoots,
        List<JudicialConnectorStructureNode> nodes,
        List<String> recommendedEntryPoints,
        Map<String, Object> metadata
) {
    public JudicialConnectorStructureReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        legacyPackageRoots = legacyPackageRoots == null ? List.of() : List.copyOf(legacyPackageRoots);
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        recommendedEntryPoints = recommendedEntryPoints == null ? List.of() : List.copyOf(recommendedEntryPoints);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt.toString());
        out.put("consolidatedPackageRoot", consolidatedPackageRoot);
        out.put("legacyPackageRoots", legacyPackageRoots);
        out.put("nodes", nodes.stream().map(JudicialConnectorStructureNode::toMap).toList());
        out.put("recommendedEntryPoints", recommendedEntryPoints);
        out.put("metadata", metadata);
        return Map.copyOf(out);
    }
}
