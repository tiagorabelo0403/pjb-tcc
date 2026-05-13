package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.util.List;

public record NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse(
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
}
