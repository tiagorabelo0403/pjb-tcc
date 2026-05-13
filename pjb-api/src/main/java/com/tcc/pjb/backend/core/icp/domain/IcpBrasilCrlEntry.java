package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilCrlEntry(String distributionPoint,
                                Instant fetchedAt,
                                long ttlSeconds) {
}
