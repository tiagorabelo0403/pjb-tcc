package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudProcessoWindowView(
        String tribunalCodigo,
        long fromProcessoId,
        int batchSize
) {}
