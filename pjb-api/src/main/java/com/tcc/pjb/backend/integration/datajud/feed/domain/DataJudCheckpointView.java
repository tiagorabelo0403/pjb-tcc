package com.tcc.pjb.backend.integration.datajud.feed.domain;

import java.time.Instant;

public record DataJudCheckpointView(String tribunalCodigo,
                                    long lastProcessoId,
                                    long totalSent,
                                    Instant lastSentAt,
                                    String lastError) {
}
