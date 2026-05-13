package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Duration;
import java.time.Instant;

public record CustodiaPrazoConsultaResult(Long custodiaId,
                                          Instant dataPrisao,
                                          Instant prazoLimite24h,
                                          Duration restante,
                                          boolean vencido) {
    public Long id() { return custodiaId; }
}
