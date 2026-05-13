package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaCutoverMatrixAggregate(
        int scoreGeral,
        boolean freezeNacionalAtivo,
        boolean prontoJanelaMaterial,
        int tribunaisLiberados,
        int competenciasLiberadas,
        List<PjbSubstituicaoFederativaCutoverTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaCutoverMatrixAggregate {
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        tribunaisLiberados = Math.max(0, tribunaisLiberados);
        competenciasLiberadas = Math.max(0, competenciasLiberadas);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
