package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.programa;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoNacionalProgramaResponse(
        int scoreGeral,
        boolean prontoOperacaoAssistida,
        boolean prontoCutoverNacional,
        boolean buildGateAprovado,
        int conectoresOperacionais,
        int conectoresBloqueados,
        int conectoresSaudaveis,
        int sistemasProntosProducao,
        List<PjbSubstituicaoNacionalOndaResponse> ondas,
        List<String> pendenciasCriticas,
        String conclusaoTecnica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoNacionalProgramaResponse {
        ondas = ondas == null ? List.of() : List.copyOf(ondas);
        pendenciasCriticas = pendenciasCriticas == null ? List.of() : List.copyOf(pendenciasCriticas);
        conclusaoTecnica = conclusaoTecnica == null ? "" : conclusaoTecnica.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
