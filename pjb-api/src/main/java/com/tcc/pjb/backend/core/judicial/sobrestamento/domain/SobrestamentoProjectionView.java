package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoProjectionView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
