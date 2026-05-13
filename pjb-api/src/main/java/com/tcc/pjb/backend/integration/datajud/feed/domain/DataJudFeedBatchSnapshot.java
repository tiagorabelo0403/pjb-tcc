package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudFeedBatchSnapshot(String tribunalCodigo, int batchSize, long cursor, boolean enabled) {}
