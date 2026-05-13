package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudErrorAuditView(
        String tribunalCodigo,
        String lastError,
        boolean failed
) {}
