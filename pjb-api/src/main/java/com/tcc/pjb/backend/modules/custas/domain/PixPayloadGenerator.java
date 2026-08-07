package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;

public interface PixPayloadGenerator {
    PixResult gerar(BigDecimal valor, Long processoId, String tipoCusta);
}
