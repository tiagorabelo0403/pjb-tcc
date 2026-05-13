package com.tcc.pjb.backend.model.dto.julgamento.safety;

import java.util.List;

public record DecisionPreflightResponse(
        String result,
        int semanticScore,
        int competingScore,
        Long competingProcessoId,
        String fingerprint,
        List<String> flags,
        String resumo
) {
}
