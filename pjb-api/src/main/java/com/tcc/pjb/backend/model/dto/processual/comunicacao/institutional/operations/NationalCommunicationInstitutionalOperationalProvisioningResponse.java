package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOperationalProvisioningResponse(
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
        List<NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse> entries,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}