package com.tcc.pjb.backend.model.dto.processual.painel.previdenciario;

public record ProcessoPainelPrevidenciarioFonteResponse(
        String code,
        String title,
        String status,
        boolean readyForUse,
        String fallbackMode,
        String signal
) {
}
