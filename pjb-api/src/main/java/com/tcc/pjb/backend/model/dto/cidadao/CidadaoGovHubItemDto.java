package com.tcc.pjb.backend.model.dto.cidadao;

public record CidadaoGovHubItemDto(
        String name,
        String serviceType,
        String url,
        CidadaoGovHubRequisitosDto requisitos
) {
}
