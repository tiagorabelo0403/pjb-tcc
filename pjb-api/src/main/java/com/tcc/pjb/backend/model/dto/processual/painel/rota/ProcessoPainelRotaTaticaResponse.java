package com.tcc.pjb.backend.model.dto.processual.painel.rota;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelRotaTaticaResponse(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        List<ProcessoPainelRotaTaticaItemResponse> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelRotaTaticaResponse {
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
