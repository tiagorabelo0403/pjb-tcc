package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCursorHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
