package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCursorHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
