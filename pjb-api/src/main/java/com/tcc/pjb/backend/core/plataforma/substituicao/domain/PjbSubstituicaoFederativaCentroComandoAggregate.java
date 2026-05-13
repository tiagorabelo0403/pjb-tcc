package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaCentroComandoAggregate(
        int scoreNacional,
        boolean prontoRolloutFederativo,
        boolean prontoRollbackGovernado,
        int tribunaisMonitorados,
        int tribunaisProntosCorteAssistido,
        int tribunaisComBloqueio,
        List<String> pendenciasCriticas,
        List<PjbSubstituicaoFederativaTribunal> tribunais,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaCentroComandoAggregate {
        pendenciasCriticas = pendenciasCriticas == null ? List.of() : List.copyOf(pendenciasCriticas);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
