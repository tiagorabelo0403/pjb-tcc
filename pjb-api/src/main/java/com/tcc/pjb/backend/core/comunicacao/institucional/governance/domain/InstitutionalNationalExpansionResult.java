package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalNationalExpansionResult(
        long regrasCriadas,
        long governancasCriadas,
        String catalogVersion,
        Instant generatedAt,
        List<String> observacoes
) {
}
