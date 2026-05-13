package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralSilencioView(
        String uf,
        LocalDate data,
        boolean emJanelaEleitoral
) {}
