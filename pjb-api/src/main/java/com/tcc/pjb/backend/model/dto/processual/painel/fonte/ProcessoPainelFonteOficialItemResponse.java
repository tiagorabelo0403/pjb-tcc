package com.tcc.pjb.backend.model.dto.processual.painel.fonte;

import java.util.List;

public record ProcessoPainelFonteOficialItemResponse(
        String widgetCode,
        String dominio,
        List<String> officialSources,
        String fallbackMode,
        String idempotencyMode,
        String replayMode,
        String forensicMode
) {
    public ProcessoPainelFonteOficialItemResponse {
        officialSources = officialSources == null ? List.of() : List.copyOf(officialSources);
    }
}
