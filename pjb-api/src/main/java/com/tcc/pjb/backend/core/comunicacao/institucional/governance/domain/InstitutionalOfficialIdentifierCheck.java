package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.util.List;
import java.util.Objects;

public record InstitutionalOfficialIdentifierCheck(
        String identifierCode,
        String identifierLabel,
        String sourceCode,
        String value,
        String normalizedValue,
        String status,
        boolean applicable,
        boolean requiredForRecognition,
        boolean readyForRemoteLookup,
        String connectorStatus,
        String officialLookupUrl,
        List<String> evidenceSignals,
        List<String> pendingIssues,
        List<String> fundamentos,
        String integrityHash
) {
    public InstitutionalOfficialIdentifierCheck {
        Objects.requireNonNull(identifierCode);
        Objects.requireNonNull(identifierLabel);
        Objects.requireNonNull(sourceCode);
        Objects.requireNonNull(status);
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
        pendingIssues = pendingIssues == null ? List.of() : List.copyOf(pendingIssues);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (integrityHash == null || integrityHash.isBlank()) {
            integrityHash = Hashes.sha256Hex(String.join("|",
                    "institutional_official_identifier_check",
                    identifierCode,
                    identifierLabel,
                    sourceCode,
                    coalesce(value),
                    coalesce(normalizedValue),
                    status,
                    String.valueOf(applicable),
                    String.valueOf(requiredForRecognition),
                    String.valueOf(readyForRemoteLookup),
                    coalesce(connectorStatus),
                    coalesce(officialLookupUrl),
                    evidenceSignals.toString(),
                    pendingIssues.toString(),
                    fundamentos.toString()
            ));
        }
    }

    private static String coalesce(String value) {
        return value == null ? "" : value;
    }
}
