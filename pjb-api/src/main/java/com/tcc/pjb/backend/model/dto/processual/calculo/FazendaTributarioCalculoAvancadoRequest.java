package com.tcc.pjb.backend.model.dto.processual.calculo;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FazendaTributarioCalculoAvancadoRequest(
        @Size(max = 180) String tituloCalculo,
        @Size(max = 64) String numeroProcesso,
        @Size(max = 120) String enteTributante,
        @Size(max = 120) String tributo,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        @Size(max = 160) String nomeSolicitante,
        @Size(max = 64) String registroProfissionalSolicitante,
        @NotNull @DecimalMin("0.00") BigDecimal principal,
        @JsonAlias("dataVencimento") @NotNull LocalDate vencimento,
        @NotNull LocalDate dataCalculo,
        @DecimalMin("0.000000") BigDecimal percentualMultaMoraDiaria,
        @DecimalMin("0.000000") BigDecimal limitePercentualMultaMora,
        @DecimalMin("0.000000") BigDecimal percentualMultaOficio,
        @DecimalMin("0.000000") BigDecimal percentualEncargoLegal,
        @DecimalMin("0.000000") BigDecimal percentualHonorarios,
        @DecimalMin("0.00") BigDecimal custas,
        Boolean aplicarMaisUmPorCentoNoMesPagamento,
        @Valid List<CalculoIndiceMensalRequest> taxasSelicMensais,
        @Valid List<CalculoParcelaLivreRequest> creditosCompensaveis,
        @Size(max = 64) String criterioCorrecaoMonetariaNome,
        @Size(max = 64) String criterioJurosNome,
        LocalDate dataInicioJurosMora,
        @DecimalMin("0.00") BigDecimal valorGarantidoOuDepositado,
        @DecimalMin("0.000000") BigDecimal percentualReducaoMulta,
        @DecimalMin("0.000000") BigDecimal percentualDescontoPrograma,
        Boolean aplicarProRataDie,
        @Size(max = 500) String observacoesTecnicas
) {
}
