package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalDraftRequest(
        String analiseV1,
        String peticaoInicialText,
        String instrucoes,
        String objetivo,
        String userProfile,
        String processoId,
        String ramo,
        String rito,
        String classe,
        List<String> attachments,
        Map<String, Object> contexto
) {
}
