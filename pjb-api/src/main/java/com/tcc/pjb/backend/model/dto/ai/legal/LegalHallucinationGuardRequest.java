package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalHallucinationGuardRequest(
        String texto,
        String ramo,
        String rito,
        String classe,
        List<String> groundedCitations,
        Map<String, Object> filtros
) {
}
