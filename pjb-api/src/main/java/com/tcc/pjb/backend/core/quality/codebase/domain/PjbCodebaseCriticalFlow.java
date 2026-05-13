package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseCriticalFlow(
        String nome,
        String status,
        double cobertura,
        List<String> testesRelacionados,
        List<String> sinais,
        List<String> acoesIniciais
) {
    public PjbCodebaseCriticalFlow {
        nome = Objects.toString(nome, "").trim();
        status = Objects.toString(status, "AUSENTE").trim();
        cobertura = Math.max(0.0d, Math.min(cobertura, 1.0d));
        testesRelacionados = testesRelacionados == null ? List.of() : List.copyOf(testesRelacionados);
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesIniciais = acoesIniciais == null ? List.of() : List.copyOf(acoesIniciais);
    }
}
