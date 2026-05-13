package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.core.financeiro.custas.domain.PixResult;
import java.math.BigDecimal;

public interface PixPayloadGenerator {
    PixResult gerar(BigDecimal valor, Long processoId, String tipoCusta);
}
