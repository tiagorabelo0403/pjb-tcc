package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalIntegrationCredentialIssueRequest(
        String affiliationId,
        String displayName,
        List<String> integrationFamilies,
        List<String> originAllowlist,
        List<String> fundamentos
) {
}
