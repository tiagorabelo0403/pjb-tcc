package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleWindowView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
