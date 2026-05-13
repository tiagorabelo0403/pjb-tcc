package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalOfficialIdentifierCheckResponse(
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
}
