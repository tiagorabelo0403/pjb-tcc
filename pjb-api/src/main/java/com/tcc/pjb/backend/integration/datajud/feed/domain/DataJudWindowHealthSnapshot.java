package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudWindowHealthSnapshot(String tribunalCodigo, long fromProcessoId, int batchSize, boolean enabled, boolean healthy) {}
