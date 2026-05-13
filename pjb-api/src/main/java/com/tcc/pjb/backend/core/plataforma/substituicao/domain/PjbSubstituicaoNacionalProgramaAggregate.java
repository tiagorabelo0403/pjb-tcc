package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoNacionalProgramaAggregate(
        int scoreGeral,
        boolean prontoOperacaoAssistida,
        boolean prontoCutoverNacional,
        boolean buildGateAprovado,
        int conectoresOperacionais,
        int conectoresBloqueados,
        int conectoresSaudaveis,
        int sistemasProntosProducao,
        List<PjbSubstituicaoNacionalOnda> ondas,
        List<String> pendenciasCriticas,
        String conclusaoTecnica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoNacionalProgramaAggregate {
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        conectoresOperacionais = Math.max(0, conectoresOperacionais);
        conectoresBloqueados = Math.max(0, conectoresBloqueados);
        conectoresSaudaveis = Math.max(0, conectoresSaudaveis);
        sistemasProntosProducao = Math.max(0, sistemasProntosProducao);
        ondas = ondas == null ? List.of() : List.copyOf(ondas);
        pendenciasCriticas = pendenciasCriticas == null ? List.of() : List.copyOf(pendenciasCriticas);
        conclusaoTecnica = Objects.toString(conclusaoTecnica, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
