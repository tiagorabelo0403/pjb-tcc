package com.tcc.pjb.backend.model.dto.criminal;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.LocalDate;

public record InqueritoCadastroRequest(
        String numeroProcedimento,
        String tipo,
        String naturezaFato,
        String resumoFatos,
        String investigadosResumo,
        String vitimasResumo,
        String indiciosResumo,
        String diligenciasPendentes,
        String orgaoApuracao,
        String uf,
        String municipio,
        NivelSigilo nivelSigilo,
        LocalDate prazoConclusao,
        Long processoVinculadoId
) {
}
