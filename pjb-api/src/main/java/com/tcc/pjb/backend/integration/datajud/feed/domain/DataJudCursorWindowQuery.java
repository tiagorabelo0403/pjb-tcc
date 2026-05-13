package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCursorWindowQuery(
        String reference,
        String scope,
        Integer limit
) {
}
