package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;

public record GerarGruTrabalhistaCommand(Long processoId,
                                         String tipo,
                                         BigDecimal valor) {
}
