package com.tcc.pjb.backend.model.dto.distribuicao;

public record DistributionWorkbenchSummaryResponse(
        String tribunalCodigo,
        String unidadeJudiciariaCodigo,
        String filaDistribuicao,
        String inboxKey,
        Integer prioridade,
        String routingRiskLevel,
        String connectorSystem,
        String competenciaTerritorialModo,
        String preventionMode,
        String faseAtual,
        String ultimoStatusOperacional
) {
}
