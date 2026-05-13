package com.tcc.pjb.backend.model.dto.security.govbr;

import java.time.Instant;
import java.util.List;

public record GovBrAccountEntryGovernanceResponse(
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
}
