package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCursorHealthView(
        String reference,
        String status,
        String summary
) {
}
