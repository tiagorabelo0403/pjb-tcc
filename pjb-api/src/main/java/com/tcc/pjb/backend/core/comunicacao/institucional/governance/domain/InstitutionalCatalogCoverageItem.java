package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;

public record InstitutionalCatalogCoverageItem(
        DestinatarioInstitucionalKind destinatarioKind,
        int totalUfsCobertas,
        int totalUnidadesAtivas,
        List<String> ufsCobertas,
        List<String> ufsFaltantes
) {
    public InstitutionalCatalogCoverageItem {
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        ufsCobertas = List.copyOf(ufsCobertas == null ? List.of() : ufsCobertas);
        ufsFaltantes = List.copyOf(ufsFaltantes == null ? List.of() : ufsFaltantes);
    }
}
