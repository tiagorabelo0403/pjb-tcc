package com.tcc.pjb.backend.core.prazos.calculo.domain;

public record PrazoConsistencyView(
        String regime,
        boolean consistent,
        String summary,
        String source
) {}
