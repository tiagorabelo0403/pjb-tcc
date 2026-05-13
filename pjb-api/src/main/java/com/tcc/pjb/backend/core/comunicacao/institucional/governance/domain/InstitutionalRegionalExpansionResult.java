package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalRegionalExpansionResult(
        String uf,
        String comarca,
        String foro,
        long regrasCriadas,
        long regrasAtualizadas,
        String catalogVersion,
        Instant generatedAt,
        List<String> notas
) {
}
