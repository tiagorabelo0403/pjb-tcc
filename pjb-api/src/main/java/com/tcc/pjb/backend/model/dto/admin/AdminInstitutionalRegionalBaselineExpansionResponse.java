package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminInstitutionalRegionalBaselineExpansionResponse(
        String uf,
        String comarca,
        String foro,
        long regrasCriadas,
        long regrasAtualizadas,
        String catalogVersion,
        Instant generatedAt,
        List<String> notas
) {
}
