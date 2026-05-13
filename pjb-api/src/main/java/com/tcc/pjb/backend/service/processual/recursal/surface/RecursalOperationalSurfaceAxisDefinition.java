package com.tcc.pjb.backend.service.processual.recursal.surface;

import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceGapView;
import java.util.List;
import java.util.Set;

public record RecursalOperationalSurfaceAxisDefinition(
        String codigo,
        String titulo,
        String rota,
        Set<String> trilhas,
        List<RecursalOperationalSurfaceGapView> gaps) {
}
