package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudFeedBulkResult(String tribunalCodigo,
                                    int totalSent,
                                    int batchesProcessed,
                                    DataJudFeedCheckpointSnapshot checkpoint,
                                    boolean success) {
}
