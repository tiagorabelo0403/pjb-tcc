package com.tcc.pjb.backend.model.dto.processual.malha;

import java.util.List;

public record ProcessoMalhaSigiloResponse(
        String nivelSigilo,
        String viewLevel,
        boolean acessoSensivel,
        boolean stepUpExigido,
        boolean stepUpAtivo,
        boolean mascarado,
        String requestId,
        List<String> fundamentos
) {
    public ProcessoMalhaSigiloResponse {
        nivelSigilo = nivelSigilo == null ? "PUBLICO" : nivelSigilo.trim();
        viewLevel = viewLevel == null ? "PUBLICO" : viewLevel.trim();
        requestId = requestId == null ? "" : requestId.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
