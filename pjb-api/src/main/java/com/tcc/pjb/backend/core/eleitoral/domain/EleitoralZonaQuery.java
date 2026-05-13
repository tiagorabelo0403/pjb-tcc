package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralZonaQuery(
        Long processoId,
        String zonaEleitoral,
        String uf
) {}
