package com.tcc.pjb.backend.core.processo.prazo.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPrazoAggregate(
        ProcessoPrazoIdentity identity,
        ProcessoPrazoCienciaProfile ciencia,
        List<ProcessoPrazoMarco> marcos,
        long totalMarcos,
        long marcosVencidos,
        long marcosCriticos,
        long marcosComCienciaObrigatoria,
        String janelaPredominante,
        List<String> proximaOndaOperacional,
        List<String> alertasEstruturais,
        Instant generatedAt
) {
    public ProcessoPrazoAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(ciencia);
        Objects.requireNonNull(janelaPredominante);
        Objects.requireNonNull(generatedAt);
        marcos = marcos == null ? List.of() : List.copyOf(marcos);
        proximaOndaOperacional = proximaOndaOperacional == null ? List.of() : List.copyOf(proximaOndaOperacional);
        alertasEstruturais = alertasEstruturais == null ? List.of() : List.copyOf(alertasEstruturais);
    }
}
