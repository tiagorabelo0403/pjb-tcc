package com.tcc.pjb.backend.model.dto.cidadao;

public record CidadaoPainelBadgesDto(
        long totalProcessos,
        int pendencias,
        int proximosEventos,
        int sigilosos
) {
}
