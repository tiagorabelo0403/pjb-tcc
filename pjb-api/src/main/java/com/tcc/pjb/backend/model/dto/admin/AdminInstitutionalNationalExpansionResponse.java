package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminInstitutionalNationalExpansionResponse(
        long regrasCriadas,
        long governancasCriadas,
        String catalogVersion,
        Instant generatedAt,
        List<String> observacoes
) {
}
