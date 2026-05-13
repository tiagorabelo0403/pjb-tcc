package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePrazoAuditView(Long djeId,
                                LocalDate disponibilizacao,
                                LocalDate publicacao,
                                LocalDate prazoComecaEm,
                                boolean partesNotificadas) {}
