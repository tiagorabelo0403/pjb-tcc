package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalAuthenticationLanePolicyResponse(
        String laneCode,
        String laneKind,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        String displayName,
        boolean requiresGovBrRootIdentity,
        String minimumGovBrLevel,
        boolean allowsInstitutionManagedLogin,
        boolean requiresInstitutionManagedLogin,
        boolean requiresMfaAtEntry,
        boolean requiresQualifiedCertificateForEntry,
        boolean requiresQualifiedCertificateForSensitiveActs,
        boolean requiresInstitutionalNetwork,
        boolean allowsRemoteAuthorizedCertificate,
        boolean signsOrSubmitsSensitiveActs,
        List<String> capacidades,
        List<String> fundamentos
) {
}
