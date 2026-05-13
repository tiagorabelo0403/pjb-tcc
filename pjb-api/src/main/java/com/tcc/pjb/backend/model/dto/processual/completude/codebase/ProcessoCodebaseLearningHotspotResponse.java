package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.util.List;

public record ProcessoCodebaseLearningHotspotResponse(
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
        List<ProcessoCodebaseLearningLaneResponse> trilhasExtracao
) {
    public ProcessoCodebaseLearningHotspotResponse {
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesRecomendadas = acoesRecomendadas == null ? List.of() : List.copyOf(acoesRecomendadas);
        trilhasExtracao = trilhasExtracao == null ? List.of() : List.copyOf(trilhasExtracao);
    }
}
