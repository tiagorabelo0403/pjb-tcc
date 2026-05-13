package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoUnificadoAto(
        String codigo,
        String titulo,
        String categoria,
        String workItemType,
        String eixoOperacional,
        String filaPadrao,
        String inboxPadrao,
        String fundamentoPadrao,
        String faseOrigem,
        String faseDestino,
        String statusOrigem,
        String statusDestino,
        boolean permitido,
        boolean recursal,
        boolean terminal,
        boolean sensivel,
        boolean exigeMagistratura,
        boolean exigeSegurancaElevada,
        String motivo,
        String responsavelSugerido,
        String transitionKey,
        List<String> alertas
) {
    public ProcessoUnificadoAto {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(categoria);
        Objects.requireNonNull(workItemType);
        Objects.requireNonNull(eixoOperacional);
        Objects.requireNonNull(transitionKey);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
    }
}
