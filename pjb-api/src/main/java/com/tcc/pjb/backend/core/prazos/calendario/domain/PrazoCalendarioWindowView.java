package com.tcc.pjb.backend.core.prazos.calendario.domain;

public record PrazoCalendarioWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
