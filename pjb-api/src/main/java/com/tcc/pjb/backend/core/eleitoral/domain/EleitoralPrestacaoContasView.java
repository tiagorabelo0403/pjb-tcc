package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralPrestacaoContasView(
        Long processoId,
        String partidoSigla,
        String cargo,
        String status
) {}
