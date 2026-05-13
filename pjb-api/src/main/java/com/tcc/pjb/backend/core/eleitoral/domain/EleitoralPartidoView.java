package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralPartidoView(
        String partidoSigla,
        int anoEleitoral,
        String tipoFeito
) {}
