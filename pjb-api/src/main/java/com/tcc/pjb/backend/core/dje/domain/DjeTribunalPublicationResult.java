package com.tcc.pjb.backend.core.dje.domain;

public record DjeTribunalPublicationResult(
        String tribunalCodigo,
        long total,
        long enviadas,
        long publicadas,
        long falhas
) {}
