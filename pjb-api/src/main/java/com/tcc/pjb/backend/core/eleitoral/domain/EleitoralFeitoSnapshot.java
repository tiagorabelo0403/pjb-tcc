package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralFeitoSnapshot(
        Long processoId,
        String tipoFeito,
        String statusEleitoral,
        LocalDate diplomadoEm
) {
}
