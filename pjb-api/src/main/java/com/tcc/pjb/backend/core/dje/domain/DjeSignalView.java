package com.tcc.pjb.backend.core.dje.domain;

import java.time.Instant;

public record DjeSignalView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
