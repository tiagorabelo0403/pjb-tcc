package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;

public record RegistrarDepositoRecursalCommand(Long processoId,
                                               String instancia,
                                               BigDecimal valorDepositado,
                                               String comprovanteHash) {
}
