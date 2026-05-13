package com.tcc.pjb.backend.model.dto.procuradoria.surface;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PrecatorioRpvCalculoRequest(
        @NotNull Long processoId,
        BigDecimal valorPrincipal,
        BigDecimal indiceCorrecao,
        BigDecimal indiceJuros,
        BigDecimal indiceSelicTeto,
        BigDecimal limiteRpv,
        PrecatorioRpvNaturezaCredito naturezaCredito,
        PrecatorioRpvEnteDevedorTipo enteDevedorTipo,
        String entidadeDevedoraCodigo,
        LocalDate dataBaseCalculo,
        LocalDate dataApresentacao,
        LocalDate dataNascimentoBeneficiario,
        boolean doencaGrave,
        boolean pessoaComDeficiencia,
        boolean regimeEspecial,
        boolean acordoDiretoHabilitado
) {
}
