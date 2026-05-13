package com.tcc.pjb.backend.core.processo.painel.domain;

import java.util.List;

public record ProcessoPainelFonteOficialItem(
        String widgetCode,
        String dominio,
        List<String> officialSources,
        String fallbackMode,
        String idempotencyMode,
        String replayMode,
        String forensicMode
) {
    public ProcessoPainelFonteOficialItem {
        officialSources = officialSources == null ? List.of() : List.copyOf(officialSources);
        dominio = dominio == null ? "GERAL" : dominio;
        fallbackMode = fallbackMode == null ? "MANUAL_ASSISTIDO" : fallbackMode;
        idempotencyMode = idempotencyMode == null ? "CHAVE_DE_NEGOCIO" : idempotencyMode;
        replayMode = replayMode == null ? "REPLAY_CONTROLADO" : replayMode;
        forensicMode = forensicMode == null ? "TRILHA_IMUTAVEL" : forensicMode;
    }
}
