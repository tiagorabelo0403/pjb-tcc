package com.tcc.pjb.backend.core.processo.painel.domain;

public record ProcessoPainelPrevidenciarioFonte(
        String code,
        String title,
        String status,
        boolean readyForUse,
        String fallbackMode,
        String signal
) {
    public ProcessoPainelPrevidenciarioFonte {
        title = title == null ? code : title;
        status = status == null ? "PARCIAL" : status;
        fallbackMode = fallbackMode == null ? "MANUAL_ASSISTIDO" : fallbackMode;
        signal = signal == null ? "" : signal;
    }
}
