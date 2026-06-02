package com.tcc.pjb.backend.model.dto.governance;

import io.swagger.v3.oas.annotations.media.Schema;
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
            @Schema(description = "Data/hora em que o evento ocorreu", format = "date-time",
                    example = "2026-06-01T10:00:00-03:00") String ocorridoEm,
            String ator,
            String resumo,
            String hash,
            String correlacao
    ) {
    }
}
