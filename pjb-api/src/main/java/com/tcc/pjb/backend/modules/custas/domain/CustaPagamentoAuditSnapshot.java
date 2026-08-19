package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CustaPagamentoAuditSnapshot(Long custaId,
                                          String status,
                                          BigDecimal valorPago,
                                          Instant pagoEm) {}
