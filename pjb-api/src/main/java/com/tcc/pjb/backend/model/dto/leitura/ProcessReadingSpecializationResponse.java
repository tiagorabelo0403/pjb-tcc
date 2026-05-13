package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingSpecializationResponse(
        String scopeCode,
        String chamberMode,
        String decisionMode,
        String evidenceMode,
        String resourceMode,
        String embargoMode,
        String hearingMode,
        String executionMode,
        String serviceDeskMode,
        boolean nativeHtmlPriority,
        boolean signedPdfInspectionRequired,
        List<String> openingSequence,
        List<String> preferredActModes,
        List<String> markers,
        Map<String, Object> metadata
) {
    public ProcessReadingSpecializationResponse {
        openingSequence = openingSequence == null ? List.of() : List.copyOf(openingSequence);
        preferredActModes = preferredActModes == null ? List.of() : List.copyOf(preferredActModes);
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
