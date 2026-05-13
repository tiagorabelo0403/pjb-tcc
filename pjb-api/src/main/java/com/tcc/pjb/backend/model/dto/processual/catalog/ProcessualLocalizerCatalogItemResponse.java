package com.tcc.pjb.backend.model.dto.processual.catalog;

public record ProcessualLocalizerCatalogItemResponse(
        String codigo,
        String titulo,
        String perfilAlvo,
        String gatilho,
        int prioridade) {
}
