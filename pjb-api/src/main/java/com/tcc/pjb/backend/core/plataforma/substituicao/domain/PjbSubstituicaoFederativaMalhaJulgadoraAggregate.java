package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaMalhaJulgadoraAggregate(
        int scoreNacional,
        boolean malhaJulgadoraPronta,
        boolean incidentesConectados,
        boolean colegiadosConectados,
        boolean unidadesJulgadorasConectadas,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaMalhaJulgadoraTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaMalhaJulgadoraAggregate {
        scoreNacional = Math.max(0, Math.min(100, scoreNacional));
        tribunaisProntos = Math.max(0, tribunaisProntos);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
