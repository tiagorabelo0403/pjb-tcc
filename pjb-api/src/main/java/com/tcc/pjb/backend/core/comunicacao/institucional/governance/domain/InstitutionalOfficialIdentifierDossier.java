package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalOfficialIdentifierDossier(
        String subjectType,
        String subjectId,
        String affiliationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        String overallStatus,
        boolean materialEvidenceReady,
        Instant generatedAt,
        List<String> blockingIssues,
        List<InstitutionalOfficialIdentifierCheck> checks,
        List<String> fundamentos,
        String integrityHash
) {
    public InstitutionalOfficialIdentifierDossier {
        Objects.requireNonNull(subjectType);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(overallStatus);
        Objects.requireNonNull(generatedAt);
        blockingIssues = blockingIssues == null ? List.of() : List.copyOf(blockingIssues);
        checks = checks == null ? List.of() : List.copyOf(checks);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (integrityHash == null || integrityHash.isBlank()) {
            integrityHash = Hashes.sha256Hex(String.join("|",
                    "institutional_official_identifier_dossier",
                    subjectType,
                    subjectId,
                    coalesce(affiliationId),
                    coalesce(requestId),
                    coalesce(organizationScope),
                    coalesce(orgaoSigla),
                    coalesce(unidadeCodigo),
                    overallStatus,
                    String.valueOf(materialEvidenceReady),
                    generatedAt.toString(),
                    blockingIssues.toString(),
                    checks.stream().map(InstitutionalOfficialIdentifierCheck::integrityHash).reduce("", String::concat),
                    fundamentos.toString()
            ));
        }
    }

    private static String coalesce(String value) {
        return value == null ? "" : value;
    }
}
