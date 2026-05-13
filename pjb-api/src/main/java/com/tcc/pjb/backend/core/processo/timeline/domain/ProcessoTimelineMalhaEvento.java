package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelineMalhaEvento(
        String codigo,
        String dominio,
        String severidade,
        Instant instante,
        boolean bloqueante,
        String titulo,
        String detalhe,
        String alvo,
        List<String> fundamentos
) {
    public ProcessoTimelineMalhaEvento {
        codigo = Objects.toString(codigo, "").trim();
        dominio = Objects.toString(dominio, "").trim();
        severidade = Objects.toString(severidade, "").trim();
        instante = instante == null ? Instant.now() : instante;
        titulo = Objects.toString(titulo, "").trim();
        detalhe = Objects.toString(detalhe, "").trim();
        alvo = Objects.toString(alvo, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    public String eixo() {
        return dominio();
    }

    public String acao() {
        return detalhe();
    }

    public String navigationPath() {
        return alvo();
    }
}
