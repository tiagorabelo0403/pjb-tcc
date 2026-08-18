package com.tcc.pjb.backend.modules.custas.domain;

public record CustaStatusWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
