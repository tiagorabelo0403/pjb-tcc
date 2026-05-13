package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.util.List;

public record ProcessoCodebaseCriticalFlowResponse(
        String nome,
        String status,
        double cobertura,
        List<String> testesRelacionados,
        List<String> sinais,
        List<String> acoesIniciais
) {
    public ProcessoCodebaseCriticalFlowResponse {
        testesRelacionados = testesRelacionados == null ? List.of() : List.copyOf(testesRelacionados);
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesIniciais = acoesIniciais == null ? List.of() : List.copyOf(acoesIniciais);
    }
}
