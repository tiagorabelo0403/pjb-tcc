package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudPayloadHashView(
        String tribunalCodigo,
        Long processoId,
        String numeroUnificado
) {}
