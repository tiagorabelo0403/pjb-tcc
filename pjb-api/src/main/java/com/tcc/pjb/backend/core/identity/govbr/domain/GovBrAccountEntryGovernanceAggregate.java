package com.tcc.pjb.backend.core.identity.govbr.domain;

import java.time.Instant;
import java.util.List;

public record GovBrAccountEntryGovernanceAggregate(
        boolean enabled,
        boolean mockEnabled,
        boolean authorizeConfigured,
        boolean tokenConfigured,
        boolean userInfoConfigured,
        boolean jwksConfigured,
        boolean issuerConfigured,
        boolean redirectPrincipalSeguro,
        boolean redirectStepUpSeguro,
        boolean dominiosOficiaisCompativeis,
        Long currentUserId,
        boolean contaGovBrVinculada,
        boolean govEmailVerificado,
        boolean govTelefoneVerificado,
        boolean contextoInstitucionalPronto,
        String redirectPrincipalHost,
        String redirectStepUpHost,
        List<String> contextosDelegadosAtivos,
        List<String> blockers,
        List<String> warnings,
        List<String> garantias,
        Instant generatedAt
) {
    public GovBrAccountEntryGovernanceAggregate {
        contextosDelegadosAtivos = contextosDelegadosAtivos == null ? List.of() : List.copyOf(contextosDelegadosAtivos);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        garantias = garantias == null ? List.of() : List.copyOf(garantias);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
