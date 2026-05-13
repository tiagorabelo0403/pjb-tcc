package com.tcc.pjb.backend.core.identity.govbr.domain;

import java.time.Instant;
import java.util.List;

public record GovBrIdentityAssuranceAggregate(
        boolean enabled,
        Long currentUserId,
        boolean contaGovBrVinculada,
        boolean contextoInstitucionalFechado,
        boolean callbackSeguro,
        boolean dominiosOficiaisCompativeis,
        boolean tokenVerificationReady,
        boolean trustedDeviceAtivo,
        boolean strongBindingReady,
        String nivelGarantia,
        List<String> blockers,
        List<String> warnings,
        List<String> garantias,
        Instant generatedAt
) {
    public GovBrIdentityAssuranceAggregate {
        nivelGarantia = nivelGarantia == null ? "INDISPONIVEL" : nivelGarantia;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        garantias = garantias == null ? List.of() : List.copyOf(garantias);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
