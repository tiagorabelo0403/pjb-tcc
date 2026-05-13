package com.tcc.pjb.backend.model.dto.admin;

public record AdminInstitutionalCatalogGovernanceSummaryResponse(
        long totalUnidadesCatalogadas,
        long totalGovernancasAtivas,
        long totalRegrasCompetenciaAtivas,
        long totalUnidadesSuspensas,
        long totalComSubstituicao,
        long totalExpirandoEm30Dias,
        String catalogVersion
) {
}
