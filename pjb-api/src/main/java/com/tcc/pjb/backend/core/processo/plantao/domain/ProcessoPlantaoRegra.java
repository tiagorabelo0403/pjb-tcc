package com.tcc.pjb.backend.core.processo.plantao.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPlantaoRegra(
        String ruleId,
        String tipoCobertura,
        String status,
        Long titularUsuarioId,
        Long coberturaUsuarioId,
        Instant inicioVigencia,
        Instant fimVigencia,
        List<String> capacidades,
        int precedencia,
        String motivo
) {
    public ProcessoPlantaoRegra {
        ruleId = ruleId == null || ruleId.isBlank() ? "SEM_REGRA" : ruleId;
        tipoCobertura = tipoCobertura == null || tipoCobertura.isBlank() ? "ROTINA" : tipoCobertura;
        status = status == null || status.isBlank() ? "ATIVA" : status;
        capacidades = capacidades == null ? List.of() : List.copyOf(capacidades);
        precedencia = Math.max(0, precedencia);
        motivo = motivo == null ? "" : motivo;
    }
}
