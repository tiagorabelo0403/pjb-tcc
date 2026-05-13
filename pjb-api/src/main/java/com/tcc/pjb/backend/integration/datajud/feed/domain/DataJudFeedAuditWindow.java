package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudFeedAuditWindow(String tribunalCodigo, long fromProcessoId, int batchSize, boolean enabled) {}
