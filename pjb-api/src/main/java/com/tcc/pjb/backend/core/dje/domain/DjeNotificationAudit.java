package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeNotificationAudit(Long djeId,
                                   Long processoId,
                                   boolean notificadas,
                                   LocalDate prazoComecaEm) {}
