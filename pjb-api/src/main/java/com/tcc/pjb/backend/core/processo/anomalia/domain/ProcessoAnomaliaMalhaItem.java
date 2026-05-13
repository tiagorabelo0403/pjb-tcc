package com.tcc.pjb.backend.core.processo.anomalia.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoAnomaliaMalhaItem(
        String codigo,
        String categoria,
        int score,
        String nivel,
        boolean exigeEscalonamento,
        String titulo,
        String detalhe,
        List<String> fundamentos
) {
    public ProcessoAnomaliaMalhaItem {
        codigo = Objects.toString(codigo, "").trim();
        categoria = Objects.toString(categoria, "").trim();
        score = Math.max(0, Math.min(100, score));
        nivel = Objects.toString(nivel, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        detalhe = Objects.toString(detalhe, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }


    public String eixo() {
        return categoria();
    }

    public String dominio() {
        return eixo();
    }
}
