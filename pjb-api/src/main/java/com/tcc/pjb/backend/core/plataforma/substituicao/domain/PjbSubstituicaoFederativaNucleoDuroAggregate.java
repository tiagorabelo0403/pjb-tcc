package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaNucleoDuroAggregate(
        int scoreNacional,
        boolean prontoNucleoDuro,
        boolean comunicacaoSigiloConectados,
        boolean prevencaoRedistribuicaoConectadas,
        boolean fluxoRecursalConectado,
        int tribunaisProntosNucleoDuro,
        List<PjbSubstituicaoFederativaNucleoDuroTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaNucleoDuroAggregate {
        scoreNacional = Math.max(0, Math.min(100, scoreNacional));
        tribunaisProntosNucleoDuro = Math.max(0, tribunaisProntosNucleoDuro);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
