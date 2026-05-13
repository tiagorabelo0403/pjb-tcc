package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;

public record AdminInstitutionalCatalogCoverageItemResponse(
        String destinatarioKind,
        int totalUfsCobertas,
        int totalUnidadesAtivas,
        List<String> ufsCobertas,
        List<String> ufsFaltantes
) {
}
