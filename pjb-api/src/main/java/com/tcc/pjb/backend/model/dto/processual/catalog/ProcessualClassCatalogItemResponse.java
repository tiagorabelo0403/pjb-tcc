package com.tcc.pjb.backend.model.dto.processual.catalog;

public record ProcessualClassCatalogItemResponse(
        int codigoTpu,
        String nome,
        String descricao,
        String ramoDireito,
        String ramoJustica,
        String faixaProcedimental,
        boolean exigeMp,
        boolean exigeSigilo) {
}
