package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaAuditoriaSnapshot(Long custodiaId,
                                        Long processoId,
                                        String status,
                                        Instant auditAt) {}
