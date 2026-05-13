package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilOcspResult(boolean revoked, Instant revokedAt) {
    public static IcpBrasilOcspResult good() {
        return new IcpBrasilOcspResult(false, null);
    }
}
