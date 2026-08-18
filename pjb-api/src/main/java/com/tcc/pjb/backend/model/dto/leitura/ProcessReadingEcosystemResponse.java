package com.tcc.pjb.backend.model.dto.leitura;

import com.tcc.pjb.backend.model.dto.shared.reading.ProcessReadingEcosystemFrontendCapabilitiesDto;
import com.tcc.pjb.backend.model.dto.shared.reading.ProcessReadingEcosystemIntegrityContextDto;
import java.util.List;

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
        ProcessReadingEcosystemFrontendCapabilitiesDto frontend,
        ProcessReadingEcosystemIntegrityContextDto integrity
) {
    public ProcessReadingEcosystemResponse {
        strategicCapabilities = strategicCapabilities == null ? List.of() : List.copyOf(strategicCapabilities);
        migrationTracks = migrationTracks == null ? List.of() : List.copyOf(migrationTracks);
        productionDifferentials = productionDifferentials == null ? List.of() : List.copyOf(productionDifferentials);
    }
}
