package com.tcc.pjb.backend.model.dto.processual.trabalhista;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrabalhistaVerbaRescisoriaRequest(
        BigDecimal salarioBase,
        LocalDate admissao,
        LocalDate demissao,
        int diasTrabalhadosNoMes,
        String tipoDispensa,
        BigDecimal valorHoraExtraBase,
        int quantidadeHorasExtras,
        BigDecimal percentualHoraExtra,
        String grauInsalubridade
) {
}
