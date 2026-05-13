package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilCertificateAuditSnapshot(String subjectDn,
                                                String issuerDn,
                                                String serialHex,
                                                Instant validUntil,
                                                boolean revoked) {}
