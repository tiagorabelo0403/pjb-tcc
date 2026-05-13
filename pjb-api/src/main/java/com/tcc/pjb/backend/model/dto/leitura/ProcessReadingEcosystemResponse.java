package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingEcosystemResponse(
        Long processoId,
        String tribunalCode,
        String tribunalName,
        String primarySystem,
        String fallbackSystem,
        String convergenceMode,
        String legacyMigrationMode,
        String browserAccessMode,
        String signatureMode,
        String mfaMode,
        String documentPipelineMode,
        String ocrMode,
        String aiAssistMode,
        String deadlineAggregationMode,
        List<String> strategicCapabilities,
        List<String> migrationTracks,
        List<String> productionDifferentials,
        Map<String, Object> frontend,
        Map<String, Object> integrity
) {
    public ProcessReadingEcosystemResponse {
        strategicCapabilities = strategicCapabilities == null ? List.of() : List.copyOf(strategicCapabilities);
        migrationTracks = migrationTracks == null ? List.of() : List.copyOf(migrationTracks);
        productionDifferentials = productionDifferentials == null ? List.of() : List.copyOf(productionDifferentials);
        frontend = frontend == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(frontend));
        integrity = integrity == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(integrity));
    }
}
