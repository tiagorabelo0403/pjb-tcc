package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudFeedWindowCommand(String tribunalCodigo, long fromProcessoId, int batchSize) {}
