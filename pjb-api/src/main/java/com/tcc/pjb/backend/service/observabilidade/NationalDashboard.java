package com.tcc.pjb.backend.service.observabilidade;

import java.time.Instant;
import java.util.List;

public record NationalDashboard(
        Instant timestamp,
        long totalProcessosNacionais,
        long workItemsPendentes,
        long workItemsExpirados,
        long workItemsConcluidos,
        long throughputUltimaHora,
        long throughputUltimas24h,
        double taxaExpiracaoPercent,
        String nivelSaude,
        List<MetricaTribunal> metricasPorUf,
        List<String> filasComBacklog,
        List<AlertaOperacional> alertas
) {}
