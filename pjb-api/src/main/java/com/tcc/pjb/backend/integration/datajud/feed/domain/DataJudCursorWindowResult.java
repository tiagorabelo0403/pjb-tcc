package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCursorWindowResult(
        boolean available,
        String summary,
        Long total
) {
}
