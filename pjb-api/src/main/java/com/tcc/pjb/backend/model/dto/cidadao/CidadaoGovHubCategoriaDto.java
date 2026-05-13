package com.tcc.pjb.backend.model.dto.cidadao;

import java.util.List;

public record CidadaoGovHubCategoriaDto(
        String key,
        String title,
        List<CidadaoGovHubItemDto> items
) {
}
