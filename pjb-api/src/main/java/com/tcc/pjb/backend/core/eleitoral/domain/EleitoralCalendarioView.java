package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralCalendarioView(
        String uf,
        LocalDate data,
        String status
) {}
