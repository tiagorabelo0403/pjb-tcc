package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaPosColetivaAggregate(
        int scoreNacional,
        boolean malhaPosColetivaPronta,
        boolean coisaJulgadaColetivaGovernada,
        boolean liquidacaoColetivaGovernada,
        boolean habilitacaoIndividualGovernada,
        boolean cumprimentoPulverizadoLotesGovernado,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaPosColetivaTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaPosColetivaAggregate {
        scoreNacional = Math.max(0, Math.min(100, scoreNacional));
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = Objects.requireNonNullElseGet(geradoEm, Instant::now);
    }
}
