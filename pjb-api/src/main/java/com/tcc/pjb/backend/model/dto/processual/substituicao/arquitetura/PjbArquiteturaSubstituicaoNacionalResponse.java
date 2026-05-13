package com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura;

import java.time.Instant;
import java.util.List;

public record PjbArquiteturaSubstituicaoNacionalResponse(
        int scoreGeral,
        boolean prontoParaSubstituicaoImediata,
        boolean buildGateAprovado,
        long totalProcessos,
        long totalWorkItemsPendentes,
        long totalWorkItemsExpirados,
        long totalTribunaisCatalogados,
        long totalRitosCatalogados,
        List<PjbArquiteturaSubstituicaoPilarResponse> pilares,
        String conclusaoTecnica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbArquiteturaSubstituicaoNacionalResponse {
        pilares = pilares == null ? List.of() : List.copyOf(pilares);
        conclusaoTecnica = conclusaoTecnica == null ? "" : conclusaoTecnica.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
