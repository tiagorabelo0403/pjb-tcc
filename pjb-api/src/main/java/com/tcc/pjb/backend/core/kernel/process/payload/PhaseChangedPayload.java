package com.tcc.pjb.backend.core.kernel.process.payload;

import java.time.LocalDateTime;

public record PhaseChangedPayload(
        Long processoId,
        String de,
        String para,
        String motivo,
        LocalDateTime em
) {
}
