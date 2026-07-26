package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.math.BigDecimal;

public record GerarCustaJudicialCommand(Long processoId, TipoCusta tipoCusta, BigDecimal valor) {
}
