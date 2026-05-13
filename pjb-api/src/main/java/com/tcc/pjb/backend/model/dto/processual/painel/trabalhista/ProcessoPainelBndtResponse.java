package com.tcc.pjb.backend.model.dto.processual.painel.trabalhista;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelBndtResponse(
        Long processoId,
        String numeroProcesso,
        boolean aplicavel,
        String status,
        boolean consultaTempoReal,
        String fonteOficial,
        String fallbackMode,
        List<String> alertas,
        List<String> proximosPassos,
        Instant geradoEm
) {
    public ProcessoPainelBndtResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximosPassos = proximosPassos == null ? List.of() : List.copyOf(proximosPassos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
