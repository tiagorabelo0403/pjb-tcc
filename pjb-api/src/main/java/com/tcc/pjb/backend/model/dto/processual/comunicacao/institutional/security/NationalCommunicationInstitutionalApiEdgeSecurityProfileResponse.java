package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String trustDomain,
        String gatewayClassName,
        String routeHostname,
        boolean gatewayApiManaged,
        boolean fapi2SecurityProfileRequired,
        boolean fapi2MessageSigningRequired,
        boolean senderConstrainedTokensRequired,
        boolean privateKeyJwtRequired,
        boolean pushedAuthorizationRequestsRequired,
        boolean pkceRequired,
        boolean mutualTlsRequired,
        boolean backendTlsPolicyRequired,
        boolean spiffeBindingRequired,
        boolean dpopAllowed,
        int recommendedCredentialRotationDays,
        List<String> workloadBindings,
        List<String> integrationFamilies,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
