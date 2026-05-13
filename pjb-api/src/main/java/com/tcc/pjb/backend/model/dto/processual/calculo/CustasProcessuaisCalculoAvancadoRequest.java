package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CustasProcessuaisCalculoAvancadoRequest(
        @Size(max = 180) String tituloCalculo,
        @Size(max = 64) String numeroProcesso,
        @Size(max = 80) String tribunal,
        @Size(max = 80) String sistemaOrigem,
        @Size(max = 120) String classeProcessual,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        @Size(max = 160) String nomeSolicitante,
        @Size(max = 64) String registroProfissionalSolicitante,
        @NotNull @DecimalMin("0.00") BigDecimal valorCausa,
        @DecimalMin("0.000000") BigDecimal percentualTaxaJudiciaria,
        @DecimalMin("0.00") BigDecimal valorMinimoTaxaJudiciaria,
        @DecimalMin("0.000000") BigDecimal percentualPreparoRecursal,
        @DecimalMin("0.00") BigDecimal despesasPostais,
        @DecimalMin("0.00") BigDecimal diligenciasOficialJustica,
        @DecimalMin("0.00") BigDecimal despesasEditais,
        @DecimalMin("0.00") BigDecimal pesquisasConveniadas,
        @DecimalMin("0.00") BigDecimal porteRemessaRetorno,
        @DecimalMin("0.00") BigDecimal custasFinaisComplementares,
        @DecimalMin("0.00") BigDecimal depositoJudicialVinculado,
        @DecimalMin("0.000000") BigDecimal fatorAtualizacaoCustas,
        LocalDate dataBaseCalculo,
        LocalDate dataFinalCalculo,
        @Size(max = 40) String unidadeReferenciaNome,
        @DecimalMin("0.00") BigDecimal valorUnidadeReferencia,
        @Size(max = 500) String observacoesTecnicas
) {
}
