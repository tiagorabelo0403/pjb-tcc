package com.tcc.pjb.backend.core.dje.domain;

public record DjeTribunalPublicationView(
        String tribunalCodigo,
        Long djeId,
        String edicao,
        String status
) {}
