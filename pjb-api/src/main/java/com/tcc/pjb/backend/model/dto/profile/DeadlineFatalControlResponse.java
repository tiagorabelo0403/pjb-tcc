package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record DeadlineFatalControlResponse(
        String actor,
        String riskLevel,
        long totalItensMonitorados,
        long totalFatais,
        long totalCriticos,
        long totalAltos,
        List<String> acoesRecomendadas,
        List<DeadlineRiskItem> itens,
        Instant geradoEm
) {

    public record DeadlineRiskItem(
            Long workItemId,
            Long processoId,
            String processoNumero,
            String titulo,
            String riskLevel,
            long diasRestantes,
            Instant prazoEm,
            Integer prioridade,
            String fila,
            String baseLegal
    ) {
    }
}
