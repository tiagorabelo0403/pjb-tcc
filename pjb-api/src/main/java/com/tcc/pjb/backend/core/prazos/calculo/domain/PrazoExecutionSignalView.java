package com.tcc.pjb.backend.core.prazos.calculo.domain;

import java.time.Instant;

public record PrazoExecutionSignalView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
