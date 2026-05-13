package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudFeedWindowResult(String tribunalCodigo, long fromProcessoId, int requestedBatchSize, boolean enabled) {}
