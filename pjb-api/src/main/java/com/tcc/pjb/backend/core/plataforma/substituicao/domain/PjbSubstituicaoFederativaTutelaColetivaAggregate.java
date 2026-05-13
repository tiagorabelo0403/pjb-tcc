package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaTutelaColetivaAggregate(
        int scoreNacional,
        boolean malhaTutelaColetivaPronta,
        boolean tutelaColetivaConectada,
        boolean demandasEstruturaisGovernadas,
        boolean execucaoColetivaGovernada,
        boolean cumprimentoMassaGovernado,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaTutelaColetivaTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaTutelaColetivaAggregate {
        scoreNacional = Math.max(0, Math.min(100, scoreNacional));
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = Objects.requireNonNullElseGet(geradoEm, Instant::now);
    }
}
