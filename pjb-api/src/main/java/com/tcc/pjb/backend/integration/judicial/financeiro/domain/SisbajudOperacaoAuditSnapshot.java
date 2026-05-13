package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record SisbajudOperacaoAuditSnapshot(Long id,
                                            Long processoId,
                                            BigDecimal valorSolicitado,
                                            String status,
                                            Instant confirmadoEm) {}
