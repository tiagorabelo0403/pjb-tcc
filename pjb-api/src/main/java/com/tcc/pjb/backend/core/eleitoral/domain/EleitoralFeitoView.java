package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralFeitoView(
        Long processoId,
        String tipoFeito,
        String status,
        LocalDate diplomadoEm
) {}
