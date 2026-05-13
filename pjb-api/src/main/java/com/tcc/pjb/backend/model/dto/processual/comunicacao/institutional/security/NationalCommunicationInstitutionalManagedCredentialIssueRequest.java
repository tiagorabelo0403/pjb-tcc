package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalManagedCredentialIssueRequest(
        String nominationId,
        Long nominatedUserId,
        String displayName,
        String laneCode,
        List<String> allowedNetworks,
        Integer rotationWindowDays,
        List<String> fundamentos
) {
}
