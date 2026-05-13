package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralCargoView(
        Long processoId,
        String cargo,
        String numeroCandidato,
        String partidoSigla
) {}
