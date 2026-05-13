package com.tcc.pjb.backend.integration.datajud.feed.domain;

import java.time.Instant;

public record DataJudCursorResult(
        String tribunalCodigo,
        long lastProcessoId,
        Instant lastSentAt,
        long totalSent,
        String lastError
) {}
