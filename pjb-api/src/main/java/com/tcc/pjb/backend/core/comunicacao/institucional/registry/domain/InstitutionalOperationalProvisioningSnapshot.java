package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOperationalProvisioningSnapshot(
        String provisioningId,
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String organizationScope,
        String blueprintCode,
        String status,
        boolean affiliationActive,
        boolean rootApprovalRequired,
        boolean rootApprovalSatisfied,
        boolean managedCredentialLaneSupported,
        boolean managedCredentialLaneReady,
        boolean trustedSignerLanePresent,
        int totalEntries,
        int totalCaixas,
        int totalLotacoes,
        int totalManagedCredentials,
        List<InstitutionalProvisionedDirectoryEntry> entries,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalOperationalProvisioningSnapshot {
        entries = entries == null ? List.of() : List.copyOf(entries);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
