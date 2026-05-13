package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
import java.math.BigDecimal;

public interface GruCodigoBarrasGenerator {
    GruResult gerar(String tipoCusta, BigDecimal valor, String uf);
}
