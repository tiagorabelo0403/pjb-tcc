package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record DecisionTrailResponse(
        Long processoId,
        String numeroProcesso,
        int totalEventos,
        List<DecisionTrailEntryView> eventos
) {
    public record DecisionTrailEntryView(
            String origem,
            String tipo,
            String ocorridoEm,
            String ator,
            String resumo,
            String hash,
            String correlacao
    ) {
    }
}
