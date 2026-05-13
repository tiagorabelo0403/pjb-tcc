package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilCertificateHealthSnapshot(String serialHex, String acSigla, boolean revoked, Instant revocationChecked, Instant validUntil) {}
