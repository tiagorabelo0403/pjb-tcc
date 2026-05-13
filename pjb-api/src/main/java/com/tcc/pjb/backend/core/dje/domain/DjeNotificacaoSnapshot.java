package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeNotificacaoSnapshot(Long djeId,
                                     Long processoId,
                                     boolean partesNotificadas,
                                     LocalDate prazoComecaEm) {
}
