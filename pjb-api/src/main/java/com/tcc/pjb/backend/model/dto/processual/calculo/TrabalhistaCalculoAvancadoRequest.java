package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrabalhistaCalculoAvancadoRequest(
        @Size(max = 180) String tituloCalculo,
        @Size(max = 64) String numeroProcesso,
        @Size(max = 160) String reclamanteNome,
        @Size(max = 160) String reclamadoNome,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        @Size(max = 160) String nomeSolicitante,
        @Size(max = 64) String registroProfissionalSolicitante,
        @NotNull @DecimalMin("0.00") BigDecimal salarioBase,
        @DecimalMin("0.00") BigDecimal remuneracaoMedia,
        @NotNull LocalDate admissao,
        @NotNull LocalDate demissao,
        @Min(0) @Max(31) Integer diasTrabalhadosNoMesRescisao,
        @Size(max = 64) String tipoDispensa,
        @DecimalMin("0.00") BigDecimal cargaHorariaMensalBase,
        @DecimalMin("0.00") BigDecimal valorHoraBaseInformado,
        @DecimalMin("0.00") BigDecimal quantidadeHorasExtras50,
        @DecimalMin("0.00") BigDecimal quantidadeHorasExtras100,
        @DecimalMin("0.00") BigDecimal quantidadeHorasNoturnas,
        @DecimalMin("0.00") BigDecimal percentualAdicionalNoturno,
        @Size(max = 32) String grauInsalubridade,
        @DecimalMin("0.00") BigDecimal baseInsalubridade,
        @DecimalMin("0.00") BigDecimal percentualPericulosidade,
        @DecimalMin("0.00") BigDecimal outrasParcelasFixasMensais,
        @Min(1) @Max(31) Integer diasUteisMediaMes,
        @Min(0) @Max(12) Integer domingosFeriadosMediaMes,
        Boolean incluirSaldoSalario,
        Boolean incluirDecimoTerceiro,
        Boolean incluirFeriasProporcionais,
        Boolean incluirAvisoPrevio,
        Boolean incluirFgtsMensal,
        Boolean incluirMultaFgts40,
        LocalDate dataInicioAtualizacao,
        LocalDate dataFimAtualizacao,
        @DecimalMin("0.000000") BigDecimal fatorPreJudicialIpcae,
        @Valid List<CalculoIndiceMensalRequest> taxasSelicMensais,
        @Valid List<CalculoParcelaLivreRequest> parcelasLivres,
        @DecimalMin("0.00") BigDecimal quantidadeHorasIntervaloIntrajornada,
        Boolean incluirReflexosEmFeriasDecimoTerceiroFgts,
        Boolean incluirInssSegurado,
        @DecimalMin("0.000000") BigDecimal percentualInssSegurado,
        Boolean incluirIrrf,
        @DecimalMin("0.000000") BigDecimal percentualIrrfEfetivo,
        Boolean aplicarMultaArt467,
        Boolean aplicarMultaArt477,
        @DecimalMin("0.000000") BigDecimal percentualHonorariosSucumbenciais,
        @Min(0) @Max(90) Integer diasAvisoPrevioInformado,
        @Size(max = 64) String criterioAtualizacaoNome,
        @Size(max = 64) String criterioJurosNome,
        @Size(max = 500) String observacoesTecnicas
) {
}
