package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudPayloadHashResult(
        Long processoId,
        String payloadHash,
        String tribunalCodigo,
        boolean indexed
) {}
