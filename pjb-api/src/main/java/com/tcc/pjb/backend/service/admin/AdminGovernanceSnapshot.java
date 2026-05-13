package com.tcc.pjb.backend.service.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminGovernanceSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        long totalProcessosNacionais,
        long workItemsPendentes,
        long workItemsExpirados,
        long workItemsConcluidos,
        long workItemsUltimaHora,
        double taxaExpiracaoPercent,
        List<String> filasComBacklog,
        List<String> alertasAtivos,
        Object sessionRisk
) {}
