package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalRecertificationCycle(
        String affiliationId,
        String organizationScope,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String status,
        long totalAdministrators,
        long activeAdministrators,
        long totalActiveNominations,
        boolean dualAdministrationRequired,
        boolean dualAdministrationSatisfied,
        boolean dueNow,
        boolean compliant,
        Instant lastVerifiedAt,
        Instant nextDueAt,
        List<String> pendingIssues,
        List<String> fundamentos,
        Instant generatedAt
) {
}
