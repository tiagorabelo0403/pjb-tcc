package com.tcc.pjb.backend.model.dto.processual.recursal.surface;

import java.util.List;

public record RecursalOperationalSurfaceSectionView(
        String codigo,
        String titulo,
        List<String> trilhas,
        List<String> secoesObrigatorias,
        List<String> alertasTaticos) {
}
