package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.util.List;

public record OficialJusticaCienciaIntimacaoRequest(
        Long challengeId,
        String otpCode,
        String status,
        String canal,
        String formaIntimacao,
        String provaResumo,
        List<String> evidenceReferences,
        String note
) {
}
