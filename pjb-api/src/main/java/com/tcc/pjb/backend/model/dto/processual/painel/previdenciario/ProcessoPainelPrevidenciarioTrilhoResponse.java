package com.tcc.pjb.backend.model.dto.processual.painel.previdenciario;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelPrevidenciarioTrilhoResponse(
        Long processoId,
        String numeroProcesso,
        boolean aplicavel,
        String statusGeral,
        String recomendacaoCnis,
        String filaPericialStatus,
        String pagamentoStatus,
        List<ProcessoPainelPrevidenciarioFonteResponse> fontes,
        List<String> alertas,
        List<String> proximosPassos,
        Instant geradoEm
) {
    public ProcessoPainelPrevidenciarioTrilhoResponse {
        fontes = fontes == null ? List.of() : List.copyOf(fontes);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximosPassos = proximosPassos == null ? List.of() : List.copyOf(proximosPassos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
