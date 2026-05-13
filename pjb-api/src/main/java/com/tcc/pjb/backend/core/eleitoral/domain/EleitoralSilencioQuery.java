package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralSilencioQuery(
        String uf,
        LocalDate data,
        boolean tutelaUrgente
) {}
