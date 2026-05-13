package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralCalendarioQuery(
        String uf,
        LocalDate data
) {}
