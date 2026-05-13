package com.tcc.pjb.backend.core.processo.runtime.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaSigiloContexto(
        NivelSigilo nivelSigilo,
        ProcessoMalhaViewLevel viewLevel,
        boolean acessoSensivel,
        boolean stepUpExigido,
        boolean stepUpAtivo,
        boolean mascarado,
        String requestId,
        List<String> fundamentos
) {
    public ProcessoMalhaSigiloContexto {
        nivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
        viewLevel = viewLevel == null ? ProcessoMalhaViewLevel.PUBLICO : viewLevel;
        requestId = Objects.toString(requestId, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
