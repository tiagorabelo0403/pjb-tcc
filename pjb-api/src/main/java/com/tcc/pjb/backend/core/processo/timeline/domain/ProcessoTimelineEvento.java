package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelineEvento(
        String codigo,
        String titulo,
        String eixo,
        long ordem,
        Instant instante,
        boolean concluido,
        boolean bloqueante,
        String responsavel,
        List<String> detalhes,
        List<String> fundamentos
) {
    public ProcessoTimelineEvento {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "PROCESSO" : eixo;
        responsavel = responsavel == null ? "SISTEMA" : responsavel;
        detalhes = detalhes == null ? List.of() : List.copyOf(detalhes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
