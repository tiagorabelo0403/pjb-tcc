package com.tcc.pjb.backend.model.dto.ministro;

public record TemaPrecedenteReconhecimentoSurfaceRequest(
        String tipo,
        String ementa,
        String abrangencia,
        String fundamentosResumo,
        double corteMinimoSimilaridade,
        int limitProcessosRelacionados
) {
}
