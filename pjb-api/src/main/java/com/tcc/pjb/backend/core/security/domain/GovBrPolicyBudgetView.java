package com.tcc.pjb.backend.core.security.domain;

public record GovBrPolicyBudgetView(
        String nivelMinimoAtoSensivel,
        String nivelMinimoAtoNormal,
        boolean stepUpDisponivel
) {}
