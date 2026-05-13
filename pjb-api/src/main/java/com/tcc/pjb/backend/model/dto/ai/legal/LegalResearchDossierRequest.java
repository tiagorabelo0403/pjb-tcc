package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.Map;

public record LegalResearchDossierRequest(
        String assunto,
        String materia,
        String contextoJuridico,
        String ramo,
        String rito,
        Map<String, Object> filtros,
        Integer topK
) {
}
