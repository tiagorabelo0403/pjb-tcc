package com.tcc.pjb.backend.model.dto.processual.completude.apisurface;

import java.util.List;

public record ProcessoApiSurfaceIssueResponse(
        String codigo,
        String severidade,
        String alvo,
        String verbo,
        String rota,
        List<String> detalhes
) {
    public ProcessoApiSurfaceIssueResponse {
        detalhes = detalhes == null ? List.of() : List.copyOf(detalhes);
    }
}
