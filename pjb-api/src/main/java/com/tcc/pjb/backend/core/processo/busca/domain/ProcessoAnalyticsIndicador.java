package com.tcc.pjb.backend.core.processo.busca.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoAnalyticsIndicador(
        String codigo,
        String titulo,
        double valor,
        String unidade,
        String estado,
        List<String> detalhes
) {
    public ProcessoAnalyticsIndicador {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        unidade = unidade == null ? "count" : unidade;
        estado = estado == null ? "INFO" : estado;
        detalhes = detalhes == null ? List.of() : List.copyOf(detalhes);
    }
}
