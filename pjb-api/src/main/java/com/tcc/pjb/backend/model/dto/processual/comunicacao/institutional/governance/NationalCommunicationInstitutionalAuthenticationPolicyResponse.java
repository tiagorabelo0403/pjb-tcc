package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalAuthenticationPolicyResponse(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String organizationScope,
        String blueprintCode,
        boolean personalRootIdentityRequired,
        boolean managedInstitutionalLoginSupported,
        boolean managedInstitutionalLoginRequiresGovBrBinding,
        boolean dualEvidenceRequiredForSensitiveActs,
        boolean qualifiedCertificateRequiredForSigners,
        boolean trustedNetworkOrRemoteAuthorizationRequiredForCertificates,
        List<NationalCommunicationInstitutionalAuthenticationLanePolicyResponse> lanePolicies,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
