package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelTelemetriaConectorAggregate(
        Long processoId,
        String numeroProcesso,
        String tribunalCodigo,
        String modoLeitura,
        List<ProcessoPainelTelemetriaConectorItem> conectores,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoPainelTelemetriaConectorAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        modoLeitura = modoLeitura == null || modoLeitura.isBlank() ? "TRIBUNAL" : modoLeitura;
        conectores = conectores == null ? List.of() : List.copyOf(conectores);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
