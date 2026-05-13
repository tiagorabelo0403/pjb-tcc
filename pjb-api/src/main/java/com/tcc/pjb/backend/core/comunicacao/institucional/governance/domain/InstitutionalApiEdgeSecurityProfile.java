package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalApiEdgeSecurityProfile(
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
    public InstitutionalApiEdgeSecurityProfile {
        workloadBindings = workloadBindings == null ? List.of() : List.copyOf(workloadBindings);
        integrationFamilies = integrationFamilies == null ? List.of() : List.copyOf(integrationFamilies);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
