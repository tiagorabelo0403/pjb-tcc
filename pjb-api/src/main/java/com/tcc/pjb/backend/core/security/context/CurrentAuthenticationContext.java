package com.tcc.pjb.backend.core.security.context;

import java.time.Instant;
import java.util.List;

public record CurrentAuthenticationContext(
        boolean authenticationPresent,
        boolean authenticated,
        boolean jwtBacked,
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
        boolean mfaActive,
        Instant generatedAt
) {
    public CurrentAuthenticationContext {
        authenticationType = authenticationType == null ? "ANONIMO" : authenticationType;
        authenticationMethod = authenticationMethod == null ? "UNKNOWN" : authenticationMethod;
        amr = amr == null ? List.of() : List.copyOf(amr);
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
