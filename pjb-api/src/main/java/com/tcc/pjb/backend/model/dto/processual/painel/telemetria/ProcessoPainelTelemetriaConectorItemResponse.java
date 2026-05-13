package com.tcc.pjb.backend.model.dto.processual.painel.telemetria;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelTelemetriaConectorItemResponse(
        String connectorCode,
        String status,
        String accentColor,
        double successRate,
        boolean submissionReady,
        boolean syncReady,
        String fallbackMode,
        String cacheMode,
        String circuitMode,
        String latencyDescriptor,
        Instant latestEventAt,
        Instant latestSuccessAt,
        List<String> sourceEndpoints,
        List<String> blockers,
        List<String> warnings
) {
    public ProcessoPainelTelemetriaConectorItemResponse {
        sourceEndpoints = sourceEndpoints == null ? List.of() : List.copyOf(sourceEndpoints);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
