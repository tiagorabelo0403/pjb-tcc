package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.math.BigDecimal;

public record SisbajudOperacaoView(Long operacaoId, BigDecimal valorSolicitado, String status, String protocoloBacen) {}
