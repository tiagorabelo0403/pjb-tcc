package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleOwnerView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
