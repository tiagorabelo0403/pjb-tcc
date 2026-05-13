package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralResultadoWindowResult(
        String uf,
        String fase,
        LocalDate inicio,
        LocalDate fim,
        boolean aberta
) {}
