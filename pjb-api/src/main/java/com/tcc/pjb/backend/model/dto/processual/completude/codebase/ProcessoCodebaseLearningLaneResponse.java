package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.util.List;

public record ProcessoCodebaseLearningLaneResponse(
        String nome,
        int arquivosMain,
        int arquivosTeste,
        double razaoTeste,
        String prontidao,
        List<String> sinais,
        List<String> acoesIniciais
) {
    public ProcessoCodebaseLearningLaneResponse {
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesIniciais = acoesIniciais == null ? List.of() : List.copyOf(acoesIniciais);
    }
}
