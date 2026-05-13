package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudFeedExecutionHealth(String tribunalCodigo, boolean enabled, boolean healthy, long totalSent) {}
