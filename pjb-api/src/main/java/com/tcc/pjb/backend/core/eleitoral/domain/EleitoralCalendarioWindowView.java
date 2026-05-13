package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralCalendarioWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
