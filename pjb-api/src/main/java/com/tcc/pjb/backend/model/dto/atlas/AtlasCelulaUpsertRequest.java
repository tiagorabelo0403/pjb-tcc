package com.tcc.pjb.backend.model.dto.atlas;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtlasCelulaUpsertRequest(
        @NotBlank @Size(min = 7, max = 7) String codigoIbge,
        @Size(max = 160) String nomeMunicipio,
        @Size(min = 2, max = 2) String uf,
        @Size(max = 30) String regiao,
        @NotNull Integer populacao,
        @NotNull Integer varasInstaladas,
        @NotNull Integer juizesEmExercicio,
        @NotNull Integer defensoriasPorMunicipio,
        @NotNull Integer advogadosOabAtivos,
        @NotNull Boolean temJuizadoEspecial,
        @NotNull Boolean temCejusc,
        @NotNull Integer processosPorMilHabitantes,
        @NotNull Integer novosProcessosMes,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxaResolutividadePct,
        @NotNull @DecimalMin("0.0") BigDecimal tempoMedioResolucaoDias,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal indiceCongestionamento,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxaJusticaGratuitaPct,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxaAutoRepresentacaoPct,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxaPrescricaoAparentePct,
        @Size(max = 60) String origemDados
) {
}
