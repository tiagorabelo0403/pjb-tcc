package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralResultadoWindowView(
        String uf,
        LocalDate from,
        LocalDate to,
        boolean dryRun
) {}
