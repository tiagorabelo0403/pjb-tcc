package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.util.List;

public record ProcessoCodebaseSanityIssueResponse(
        String codigo,
        String severidade,
        String arquivo,
        List<Integer> linhas,
        String detalhe
) {
    public ProcessoCodebaseSanityIssueResponse {
        linhas = linhas == null ? List.of() : List.copyOf(linhas);
    }
}
