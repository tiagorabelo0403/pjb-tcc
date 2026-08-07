package com.tcc.pjb.backend.modules.custas.api;

import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import java.math.BigDecimal;

public interface GruCodigoBarrasGenerator {
    GruResult gerar(String tipoCusta, BigDecimal valor, String uf);
}
