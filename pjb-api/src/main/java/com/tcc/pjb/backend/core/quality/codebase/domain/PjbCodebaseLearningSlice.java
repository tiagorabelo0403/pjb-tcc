package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseLearningSlice(
        String fatia,
        int arquivosMain,
        int arquivosTeste,
        int dependenciasEntrantes,
        int dependenciasSaida,
        int consumidoresController,
        double razaoTeste,
        int pressaoExtracao,
        String prioridade,
        List<String> sinais,
        List<String> acoesRecomendadas,
        List<PjbCodebaseExtractionLane> trilhasExtracao
) {
    public PjbCodebaseLearningSlice {
        fatia = Objects.toString(fatia, "").trim();
        prioridade = Objects.toString(prioridade, "MODERADA").trim();
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesRecomendadas = acoesRecomendadas == null ? List.of() : List.copyOf(acoesRecomendadas);
        trilhasExtracao = trilhasExtracao == null ? List.of() : List.copyOf(trilhasExtracao);
        razaoTeste = Math.max(0.0d, razaoTeste);
        pressaoExtracao = Math.max(0, pressaoExtracao);
    }
}
