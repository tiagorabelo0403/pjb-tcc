package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalCatalogCoverageSummary(
        List<InstitutionalCatalogCoverageItem> itens,
        String catalogVersion,
        Instant geradoEm
) {
    public InstitutionalCatalogCoverageSummary {
        itens = List.copyOf(itens == null ? List.of() : itens);
        catalogVersion = Objects.requireNonNull(catalogVersion, "catalogVersion");
        geradoEm = Objects.requireNonNull(geradoEm, "geradoEm");
    }
}
