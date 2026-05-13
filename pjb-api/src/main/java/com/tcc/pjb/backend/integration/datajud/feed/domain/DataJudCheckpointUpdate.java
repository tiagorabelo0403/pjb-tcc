package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCheckpointUpdate(String tribunalCodigo, long lastProcessoId, int totalSent) {
}
