package com.tcc.pjb.backend.model.dto.processual.catalog;

import java.util.List;

public record NationalProceduralCatalogResponse(
        Long processoId,
        String numeroProcesso,
        int versaoAssunto,
        String termoAplicado,
        List<ProcessualClassCatalogItemResponse> classes,
        List<ProcessualSubjectCatalogItemResponse> assuntos,
        List<ProcessualMovementCatalogItemResponse> movimentos,
        List<ProcessualLocalizerCatalogItemResponse> localizadores,
        List<String> alertas) {
}
