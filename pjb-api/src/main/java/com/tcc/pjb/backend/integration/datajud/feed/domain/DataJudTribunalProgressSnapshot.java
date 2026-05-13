package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudTribunalProgressSnapshot(String tribunalCodigo, int batchSent, long totalSent, boolean success) {}
