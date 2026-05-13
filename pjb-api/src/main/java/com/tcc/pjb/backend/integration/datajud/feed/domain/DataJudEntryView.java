package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudEntryView(
        Long processoId,
        String numeroUnificado,
        String tribunal,
        String classeTpuCodigo
) {}
