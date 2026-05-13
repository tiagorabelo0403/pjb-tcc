package com.tcc.pjb.backend.model.dto.competencia;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CompetenceResolveResponse(
        String requestId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant generatedAt,
        String tipoJusticaSugerida,
        String ritoSugerido,
        double confidence,
        List<String> reasons,
        List<String> legalBases,
        Map<String, Object> debug
) {
}
