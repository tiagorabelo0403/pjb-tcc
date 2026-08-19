package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;

public record GerarCustaJudicialCommand(Long processoId, TipoCusta tipoCusta, BigDecimal valor) {
}
