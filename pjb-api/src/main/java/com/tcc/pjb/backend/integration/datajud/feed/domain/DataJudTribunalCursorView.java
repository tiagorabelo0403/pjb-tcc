package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudTribunalCursorView(
        String tribunalCodigo,
        long lastProcessoId,
        long totalSent
) {}
