package com.tcc.pjb.backend.core.processo.integracao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoIntegracaoAggregate(
        ProcessoIntegracaoIdentity identity,
        String trilhaConnector,
        String prontidaoEnvio,
        String prontidaoShadow,
        List<ProcessoIntegracaoCanal> canais,
        List<ProcessoIntegracaoEvento> eventos,
        List<String> proximasAcoes,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoIntegracaoAggregate {
        Objects.requireNonNull(identity);
        trilhaConnector = trilhaConnector == null ? "OUTRO" : trilhaConnector;
        prontidaoEnvio = prontidaoEnvio == null ? "NAO_AVALIADO" : prontidaoEnvio;
        prontidaoShadow = prontidaoShadow == null ? "NAO_AVALIADO" : prontidaoShadow;
        canais = canais == null ? List.of() : List.copyOf(canais);
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
