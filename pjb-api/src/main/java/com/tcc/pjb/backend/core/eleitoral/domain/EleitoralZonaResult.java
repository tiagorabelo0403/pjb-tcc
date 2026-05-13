package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralZonaResult(
        Long processoId,
        String zonaEleitoral,
        String municipio,
        String uf,
        String cartorioCodigo
) {}
