package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilRevocationAuditSnapshot(boolean revoked, String source, boolean evidencePresent) {}
