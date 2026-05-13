package com.tcc.pjb.backend.core.security.domain;

public record GovBrConsistencyView(
        String nivelAtual,
        boolean consistente,
        String summary,
        String source
) {}
