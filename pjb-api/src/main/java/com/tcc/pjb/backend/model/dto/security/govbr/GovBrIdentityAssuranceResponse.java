package com.tcc.pjb.backend.model.dto.security.govbr;

import java.time.Instant;
import java.util.List;

public record GovBrIdentityAssuranceResponse(
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
}
