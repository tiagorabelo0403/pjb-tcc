package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record FeitoEleitoralStatusSnapshot(Long processoId,
                                           String statusEleitoral,
                                           LocalDate diplomadoEm) {
}
