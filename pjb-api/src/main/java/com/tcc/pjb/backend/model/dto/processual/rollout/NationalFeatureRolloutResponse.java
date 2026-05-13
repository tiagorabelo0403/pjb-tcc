package com.tcc.pjb.backend.model.dto.processual.rollout;

import java.util.List;
import java.util.Map;

public record NationalFeatureRolloutResponse(
        String featureCode,
        boolean enabled,
        String rolloutMode,
        int thresholdPercent,
        int hashBucket,
        String tribunalCodigo,
        String perfilAlvo,
        String anchor,
        List<String> fundamentos,
        List<String> warnings,
        Map<String, Object> metadata
) {
    public NationalFeatureRolloutResponse {
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
