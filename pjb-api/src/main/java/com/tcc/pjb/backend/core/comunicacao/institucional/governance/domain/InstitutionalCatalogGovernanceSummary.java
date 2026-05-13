package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

public record InstitutionalCatalogGovernanceSummary(
        long totalUnidadesCatalogadas,
        long totalGovernancasAtivas,
        long totalRegrasCompetenciaAtivas,
        long totalUnidadesSuspensas,
        long totalComSubstituicao,
        long totalExpirandoEm30Dias,
        String catalogVersion
) {
}
