package com.tcc.pjb.backend.model.dto.processual.completude.apisurface;

import java.time.Instant;
import java.util.List;

public record ProcessoApiSurfaceSanityResponse(
        boolean raizEncontrada,
        boolean limpo,
        int score,
        int controllersInspecionados,
        int dtoInspecionados,
        int rotasDuplicadas,
        int dtoForaDoPadrao,
        int exposicoesDiretasDeDominio,
        List<ProcessoApiSurfaceIssueResponse> issues,
        Instant auditadoEm
) {
    public ProcessoApiSurfaceSanityResponse {
        issues = issues == null ? List.of() : List.copyOf(issues);
        auditadoEm = auditadoEm == null ? Instant.now() : auditadoEm;
    }
}
