package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpCertRevocationView(
        String serialHex,
        boolean revoked,
        Instant checkedAt
) {}
