package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalTextClosureAudit(
        String auditId,
        boolean fullyClosed,
        int totalItems,
        int implementedItems,
        List<InstitutionalTextClosureItem> items,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalTextClosureAudit {
        items = items == null ? List.of() : List.copyOf(items);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
