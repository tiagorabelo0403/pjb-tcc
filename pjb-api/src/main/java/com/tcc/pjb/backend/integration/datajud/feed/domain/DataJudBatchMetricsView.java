package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudBatchMetricsView(
        String tribunalCodigo,
        int batchSize,
        long lastProcessoId,
        boolean enabled
) {}
