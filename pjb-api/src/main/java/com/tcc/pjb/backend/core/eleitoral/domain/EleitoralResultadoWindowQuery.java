package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralResultadoWindowQuery(
        String uf,
        LocalDate dataReferencia,
        String fase
) {}
