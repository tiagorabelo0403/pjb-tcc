package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record SisbajudConsultaResult(Long id,
                                     String status,
                                     BigDecimal valorSolicitado,
                                     String protocolo,
                                     Instant confirmadoEm) {}
