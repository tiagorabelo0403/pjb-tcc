package com.tcc.pjb.backend.model.dto.processual.recursal.surface;

import java.util.List;

public record RecursalOperationalSurfaceResponse(
        String rotaPrioritaria,
        String nomenclaturaAtiva,
        boolean bloqueado,
        String motivoBloqueio,
        List<RecursalOperationalSurfaceSectionView> secoes,
        List<RecursalOperationalSurfaceGapView> faltantes) {
}
