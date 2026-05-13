package com.tcc.pjb.backend.model.dto.processual.catalog;

public record ProcessualMovementCatalogItemResponse(
        String codigo,
        String titulo,
        String categoria,
        String workItemType,
        String filaPadrao,
        String inboxPadrao,
        String fundamentoPadrao) {
}
