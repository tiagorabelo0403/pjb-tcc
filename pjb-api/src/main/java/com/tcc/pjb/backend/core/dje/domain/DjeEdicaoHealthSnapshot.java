package com.tcc.pjb.backend.core.dje.domain;

public record DjeEdicaoHealthSnapshot(
        String edicao,
        boolean available,
        int publicacoes
) {}
