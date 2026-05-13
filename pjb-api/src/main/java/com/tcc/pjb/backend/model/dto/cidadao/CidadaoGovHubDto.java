package com.tcc.pjb.backend.model.dto.cidadao;

import java.util.List;

public record CidadaoGovHubDto(
        String uf,
        List<CidadaoGovHubCategoriaDto> categories
) {
}
