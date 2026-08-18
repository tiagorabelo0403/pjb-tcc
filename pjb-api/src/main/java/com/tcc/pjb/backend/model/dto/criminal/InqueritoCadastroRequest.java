package com.tcc.pjb.backend.model.dto.criminal;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record InqueritoCadastroRequest(
        String numeroProcedimento,
        String tipo,
        @NotBlank String naturezaFato,
        @NotBlank String resumoFatos,
        String investigadosResumo,
        String vitimasResumo,
        String indiciosResumo,
        String diligenciasPendentes,
        String orgaoApuracao,
        @NotNull @Positive Long unidadeApuracaoId,
        String uf,
        String municipio,
        NivelSigilo nivelSigilo,
        LocalDate prazoConclusao,
        Long processoVinculadoId
) {
}
