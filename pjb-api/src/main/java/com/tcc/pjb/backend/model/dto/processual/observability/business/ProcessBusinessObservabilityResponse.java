package com.tcc.pjb.backend.model.dto.processual.observability.business;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessBusinessObservabilityResponse(
        Instant generatedAt,
        long totalProcessos,
        long ativos,
        long recursais,
        long arquivados,
        long transitoJulgado,
        long workItemsPendentes,
        long workItemsEmExecucao,
        long workItemsVencidos,
        long outboxPendentes,
        long comunicacoesPendentes,
        long comunicacoesFrustradas,
        long comunicacoesEvasao,
        long comunicacoesEscalonadas,
        long caseFilesUnificados,
        long proceedingsMaterializados,
        long proceedingsRecursais,
        long proceedingsExecutorios,
        long staleProceedings,
        long caseFileEvents,
        long caseFilesAttentionRequired,
        long orphanProceedingParents,
        long divergentRootProceedings,
        Map<String, Long> processosPorRamo,
        Map<String, Long> processosPorStatus,
        List<String> filasCriticas,
        List<String> alertas
) {
    public ProcessBusinessObservabilityResponse {
        processosPorRamo = processosPorRamo == null ? Map.of() : Map.copyOf(processosPorRamo);
        processosPorStatus = processosPorStatus == null ? Map.of() : Map.copyOf(processosPorStatus);
        filasCriticas = filasCriticas == null ? List.of() : List.copyOf(filasCriticas);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
