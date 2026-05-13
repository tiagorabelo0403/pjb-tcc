package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralDiplomacaoAuditSnapshot(Long processoId,
                                               String tipoFeito,
                                               LocalDate diplomadoEm,
                                               boolean extinto) {}
