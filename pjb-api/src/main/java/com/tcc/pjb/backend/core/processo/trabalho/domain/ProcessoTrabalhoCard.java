package com.tcc.pjb.backend.core.processo.trabalho.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTrabalhoCard(
        Long workItemId,
        String titulo,
        String templateCode,
        String tipo,
        String fila,
        String inbox,
        String papel,
        String status,
        int prioridade,
        boolean bloqueante,
        boolean vencido,
        boolean venceEmAte48h,
        Instant dueAt,
        List<String> etiquetas
) {
    public ProcessoTrabalhoCard {
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(status);
        etiquetas = etiquetas == null ? List.of() : List.copyOf(etiquetas);
    }
}
