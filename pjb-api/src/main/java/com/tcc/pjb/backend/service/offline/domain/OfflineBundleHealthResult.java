package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleHealthResult(
        String bundleToken,
        String status,
        Instant expiraEm,
        boolean expirado,
        boolean sincronizado
) {}
