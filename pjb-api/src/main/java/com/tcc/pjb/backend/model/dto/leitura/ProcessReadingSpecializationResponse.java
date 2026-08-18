package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Metadados tecnicos da especializacao de leitura — varia por rito")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingSpecializationResponse {
        openingSequence = openingSequence == null ? List.of() : List.copyOf(openingSequence);
        preferredActModes = preferredActModes == null ? List.of() : List.copyOf(preferredActModes);
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

