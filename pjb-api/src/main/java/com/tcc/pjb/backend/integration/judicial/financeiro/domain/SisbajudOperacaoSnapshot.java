package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.math.BigDecimal;

public record SisbajudOperacaoSnapshot(Long operacaoId,
                                       Long processoId,
                                       String status,
                                       BigDecimal valorSolicitado,
                                       String protocoloBacen) {
}
