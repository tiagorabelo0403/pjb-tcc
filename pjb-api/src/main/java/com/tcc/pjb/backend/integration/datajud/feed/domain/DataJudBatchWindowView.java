package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudBatchWindowView(
        String reference,
        String status,
        String summary
) {
}
