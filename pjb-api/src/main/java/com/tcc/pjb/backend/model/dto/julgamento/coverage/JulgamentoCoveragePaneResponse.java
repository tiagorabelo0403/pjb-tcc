package com.tcc.pjb.backend.model.dto.julgamento.coverage;

import java.util.List;
import java.util.Map;

public record JulgamentoCoveragePaneResponse(
        String status,
        int score,
        List<String> fundamentos,
        List<String> alertas,
        List<String> bloqueios,
        Map<String, Object> metadata
) {
}
