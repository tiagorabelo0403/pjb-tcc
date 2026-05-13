package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.time.Instant;
import java.util.List;

public record ProcessoCodebaseSanityResponse(
        boolean disponivel,
        boolean limpo,
        int score,
        int arquivosEscaneados,
        int fqnsDuplicados,
        int importsInternosQuebrados,
        int virtualThreadsDiretas,
        List<String> diretoriosOrfaos,
        List<ProcessoCodebaseSanityIssueResponse> issues,
        Instant geradoEm
) {
    public ProcessoCodebaseSanityResponse {
        diretoriosOrfaos = diretoriosOrfaos == null ? List.of() : List.copyOf(diretoriosOrfaos);
        issues = issues == null ? List.of() : List.copyOf(issues);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
