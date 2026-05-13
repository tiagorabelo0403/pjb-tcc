package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;

public record LegalTriageResponse(
        String classificacao,
        List<String> keywords,
        List<String> documentosFaltantesSugeridos,
        String respostaBruta
) {
}
