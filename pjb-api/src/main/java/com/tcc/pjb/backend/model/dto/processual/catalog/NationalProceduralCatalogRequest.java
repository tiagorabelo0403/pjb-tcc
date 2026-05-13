package com.tcc.pjb.backend.model.dto.processual.catalog;

public record NationalProceduralCatalogRequest(
        String termo,
        Long processoId,
        Integer limite) {
}
