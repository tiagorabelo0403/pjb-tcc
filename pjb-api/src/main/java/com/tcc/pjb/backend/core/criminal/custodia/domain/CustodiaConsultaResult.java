package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaConsultaResult(Long id,
                                     Long processoId,
                                     String status,
                                     String resultado,
                                     Instant prazoLimite24h,
                                     Instant realizadaEm) {}
