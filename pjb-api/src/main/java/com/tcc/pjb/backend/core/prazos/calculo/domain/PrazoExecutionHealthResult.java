package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;

public record PrazoExecutionHealthResult(
        PrazoRegime regime,
        int quantidade,
        boolean healthy,
        String summary
) {}
