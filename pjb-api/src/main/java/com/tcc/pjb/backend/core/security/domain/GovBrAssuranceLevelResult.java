package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceLevelResult(
        String nivelAtual,
        boolean aptoAtoSensivel,
        boolean aptoAtoNormal,
        String decision
) {}
