package com.tcc.pjb.backend.model.dto.processual.painel.telemetria;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelTelemetriaConectorResponse(
        Long processoId,
        String numeroProcesso,
        String tribunalCodigo,
        String modoLeitura,
        List<ProcessoPainelTelemetriaConectorItemResponse> conectores,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoPainelTelemetriaConectorResponse {
        conectores = conectores == null ? List.of() : List.copyOf(conectores);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
