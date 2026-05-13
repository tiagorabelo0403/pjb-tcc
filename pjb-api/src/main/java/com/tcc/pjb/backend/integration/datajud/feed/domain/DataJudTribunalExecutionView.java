package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudTribunalExecutionView(String tribunalCodigo, long totalSent, long lastProcessoId, boolean healthy) {}
