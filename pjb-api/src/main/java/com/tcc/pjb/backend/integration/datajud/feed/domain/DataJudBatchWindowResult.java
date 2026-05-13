package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudBatchWindowResult(
        String tribunalCodigo,
        int requestedBatches,
        int processedBatches,
        long sentEntries,
        String status
) {}
