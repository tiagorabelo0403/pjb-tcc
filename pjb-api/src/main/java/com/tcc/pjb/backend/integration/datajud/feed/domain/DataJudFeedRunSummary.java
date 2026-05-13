package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudFeedRunSummary(String tribunalCodigo,
                                    int totalSent,
                                    long lastProcessoId,
                                    boolean completed) {
}
