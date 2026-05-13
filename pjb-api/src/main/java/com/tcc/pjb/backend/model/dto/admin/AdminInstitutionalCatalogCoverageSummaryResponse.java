package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminInstitutionalCatalogCoverageSummaryResponse(
        List<AdminInstitutionalCatalogCoverageItemResponse> itens,
        String catalogVersion,
        Instant geradoEm
) {
}
