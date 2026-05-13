package com.tcc.pjb.backend.core.dje.domain;

import java.time.Instant;

public record DjeStatusSnapshot(Long djeId,
                                Long processoId,
                                String status,
                                Instant enviadoEm,
                                Instant publicadoEm) {}
