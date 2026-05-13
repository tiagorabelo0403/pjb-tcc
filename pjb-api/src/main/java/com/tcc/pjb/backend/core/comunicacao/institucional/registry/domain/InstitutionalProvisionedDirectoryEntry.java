package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.util.List;

public record InstitutionalProvisionedDirectoryEntry(
        String entryId,
        String entryType,
        String parentEntryId,
        String code,
        String displayName,
        String organizationalScope,
        String territorialScope,
        String caixaCodigo,
        Long userId,
        String userName,
        String horizontalDataPlaneKey,
        String primaryWritePartitionKey,
        String readReplicaCode,
        boolean active,
        List<String> findings,
        List<String> fundamentos
) {
    public InstitutionalProvisionedDirectoryEntry {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
