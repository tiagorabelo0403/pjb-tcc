package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudCheckpointHealthView(String tribunalCodigo, long lastProcessoId, long totalSent, boolean healthy) {}
