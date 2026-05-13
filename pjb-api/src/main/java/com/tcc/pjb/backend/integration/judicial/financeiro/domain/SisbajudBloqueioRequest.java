package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.math.BigDecimal;

public record SisbajudBloqueioRequest(Long processoId,
                                      String cpfDevedor,
                                      BigDecimal valorSolicitado,
                                      String numeroOficio,
                                      boolean delegatedOperation) {
}
