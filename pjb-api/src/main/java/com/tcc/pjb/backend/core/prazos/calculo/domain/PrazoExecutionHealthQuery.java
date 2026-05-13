package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;

public record PrazoExecutionHealthQuery(
        PrazoRegime regime,
        int quantidade,
        String uf,
        String comarca
) {}
