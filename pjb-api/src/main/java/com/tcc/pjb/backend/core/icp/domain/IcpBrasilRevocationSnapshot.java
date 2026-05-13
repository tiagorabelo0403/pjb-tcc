package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilRevocationSnapshot(boolean revoked,
                                          Instant revokedAt,
                                          String source) {
}
