package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralZonaProcessoView(
        Long processoId,
        String zonaEleitoral,
        String municipio,
        String uf
) {}
