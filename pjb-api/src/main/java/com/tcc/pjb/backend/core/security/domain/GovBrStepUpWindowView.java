package com.tcc.pjb.backend.core.security.domain;

public record GovBrStepUpWindowView(
        String nivelAtual,
        boolean required,
        String reason,
        String targetLevel
) {}
