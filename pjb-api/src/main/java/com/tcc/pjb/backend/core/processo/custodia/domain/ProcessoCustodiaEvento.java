package com.tcc.pjb.backend.core.processo.custodia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoCustodiaEvento(
        String codigo,
        String titulo,
        Instant instante,
        String ator,
        String ledgerHash,
        List<String> fundamentos
) {
    public ProcessoCustodiaEvento {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        ator = Objects.toString(ator, "").trim();
        ledgerHash = Objects.toString(ledgerHash, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        instante = instante == null ? Instant.now() : instante;
    }
}
