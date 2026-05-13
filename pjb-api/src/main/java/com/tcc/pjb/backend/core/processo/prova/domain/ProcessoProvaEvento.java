package com.tcc.pjb.backend.core.processo.prova.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoProvaEvento(
        String codigo,
        String titulo,
        Instant instante,
        String responsavel,
        String hash,
        boolean lacrada,
        List<String> fundamentos
) {
    public ProcessoProvaEvento {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        responsavel = Objects.toString(responsavel, "").trim();
        hash = Objects.toString(hash, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        instante = instante == null ? Instant.EPOCH : instante;
    }
}
