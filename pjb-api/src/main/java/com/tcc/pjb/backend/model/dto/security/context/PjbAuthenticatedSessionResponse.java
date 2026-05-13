package com.tcc.pjb.backend.model.dto.security.context;

import java.time.Instant;
import java.util.List;

public record PjbAuthenticatedSessionResponse(
        boolean authenticated,
        boolean jwtBacked,
        boolean mfaAtivo,
        String authenticationType,
        String authenticationMethod,
        String principalName,
        String principalSubject,
        String principalIssuer,
        String principalUid,
        String principalCpf,
        String principalEmail,
        String acr,
        List<String> amr,
        List<String> authorities,
        Long activeDeviceId,
        String govBrNivelGarantia,
        boolean contaGovBrVinculada,
        boolean trustedDeviceAtivo,
        boolean contextoInstitucionalFechado,
        String affiliationId,
        String nominationId,
        String panelCode,
        String landingPath,
        String profileState,
        String targetEnvironment,
        String readReplicaCode,
        boolean institutionalProfileVisible,
        boolean readyForInstitutionalPanel,
        boolean panelProvisioningComplete,
        boolean sharedExperienceReady,
        boolean activateInstitutionalContext,
        List<String> evidencias,
        Instant generatedAt
) {
    public PjbAuthenticatedSessionResponse {
        amr = amr == null ? List.of() : List.copyOf(amr);
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
