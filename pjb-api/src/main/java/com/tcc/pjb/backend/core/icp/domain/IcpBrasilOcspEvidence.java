package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilOcspEvidence(boolean checked,
                                    boolean revoked,
                                    Instant revokedAt,
                                    String transport) {
}
