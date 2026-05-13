package com.tcc.pjb.backend.core.prazos.calculo.domain;

public record PrazoWindowHealthView(
        String regime,
        String uf,
        String comarca,
        boolean healthy,
        String summary
) {}
