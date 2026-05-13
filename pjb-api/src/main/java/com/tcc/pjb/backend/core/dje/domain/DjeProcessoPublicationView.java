package com.tcc.pjb.backend.core.dje.domain;

public record DjeProcessoPublicationView(
        Long processoId,
        Long djeId,
        String status,
        String tribunalCodigo
) {}
