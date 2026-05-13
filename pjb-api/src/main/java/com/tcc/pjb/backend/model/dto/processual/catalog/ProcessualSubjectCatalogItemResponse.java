package com.tcc.pjb.backend.model.dto.processual.catalog;

import java.util.List;

public record ProcessualSubjectCatalogItemResponse(
        String id,
        String label,
        String descricao,
        String icon,
        String pattern,
        List<String> matchAny) {
}
