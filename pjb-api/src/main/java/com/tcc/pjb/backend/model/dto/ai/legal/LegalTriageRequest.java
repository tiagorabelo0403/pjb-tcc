package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.Map;

public record LegalTriageRequest(
        String assunto,
        String materia,
        String contextoJuridico,
        Map<String, Object> filtros,
        Integer topK
) {
}
