package com.tcc.pjb.backend.core.processo.trabalho.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTrabalhoFila(
        String codigo,
        String titulo,
        long total,
        long pendentes,
        long bloqueantes,
        long vencidos,
        Instant proximoVencimento,
        List<String> papeisEnvolvidos,
        List<ProcessoTrabalhoCard> cards
) {
    public ProcessoTrabalhoFila {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        papeisEnvolvidos = papeisEnvolvidos == null ? List.of() : List.copyOf(papeisEnvolvidos);
        cards = cards == null ? List.of() : List.copyOf(cards);
    }
}
