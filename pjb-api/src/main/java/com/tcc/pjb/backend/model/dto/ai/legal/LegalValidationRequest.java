package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.Map;

public record LegalValidationRequest(
        String texto,
        String ramo,
        String rito,
        String classe,
        String objetivo,
        String sigilo,
        Map<String, Object> filtros
) {
}
