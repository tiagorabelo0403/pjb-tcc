package com.tcc.pjb.backend.service.observabilidade;

public record MetricaTribunal(
        String uf,
        long totalProcessos,
        long workItemsPendentes,
        long workItemsExpirados
) {}
