package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FederalPrevidenciarioCjfCalculoAvancadoRequest(
        @Size(max = 180) String tituloCalculo,
        @Size(max = 64) String numeroProcesso,
        @Size(max = 80) String tribunal,
        @Size(max = 80) String sistemaOrigem,
        @Size(max = 120) String tipoBeneficio,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        @Size(max = 160) String nomeSolicitante,
        @Size(max = 64) String registroProfissionalSolicitante,
        @NotNull @DecimalMin("0.00") BigDecimal rendaMensalAtual,
        @NotNull LocalDate dib,
        LocalDate dip,
        LocalDate dcb,
        LocalDate dataAjuizamento,
        LocalDate dataCitacao,
        @NotNull LocalDate dataCalculo,
        Boolean aplicarPrescricaoQuinquenal,
        Boolean incluirAbonoAnual,
        @DecimalMin("0.00") BigDecimal parcelasPagasAdministrativamente,
        @DecimalMin("0.00") BigDecimal parcelasPagasPorTutela,
        List<CalculoIndiceMensalRequest> taxasCorrecaoMensais,
        @DecimalMin("0.000000") BigDecimal fatorCorrecaoMonetaria,
        @DecimalMin("0.000000") BigDecimal percentualJurosMoraMensal,
        @DecimalMin("0.000000") BigDecimal percentualHonorarios,
        @DecimalMin("0.00") BigDecimal salarioMinimoReferencia,
        @DecimalMin("0.00") BigDecimal tetoRpvEmSalariosMinimos,
        @Size(max = 80) String criterioAtualizacaoNome,
        @Size(max = 80) String criterioJurosNome,
        @Size(max = 500) String observacoesTecnicas
) {
}
