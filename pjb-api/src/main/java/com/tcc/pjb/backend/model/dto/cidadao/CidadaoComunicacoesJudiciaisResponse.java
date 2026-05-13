package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoComunicacoesJudiciaisResponse(
        LocalDateTime generatedAt,
        int total,
        int pendentesCiencia,
        List<CidadaoComunicacaoJudicialDto> itens,
        String legendUrl,
        AreaLinks links
) {
}
