package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelinePendencia(
        String codigo,
        String titulo,
        String categoria,
        String criticidade,
        Instant prazo,
        String responsavel,
        boolean bloqueiaProximoPasso,
        List<String> fundamentos
) {
    public ProcessoTimelinePendencia {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        categoria = categoria == null ? "OPERACIONAL" : categoria;
        criticidade = criticidade == null ? "CONTROLADA" : criticidade;
        responsavel = responsavel == null ? "NAO_DEFINIDO" : responsavel;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
