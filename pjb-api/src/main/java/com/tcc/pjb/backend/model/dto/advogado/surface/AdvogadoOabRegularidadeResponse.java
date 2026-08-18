package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.time.Instant;

public record AdvogadoOabRegularidadeResponse(
        String status,
        String reasonCode,
        String source,
        Instant checkedAt
) {}
