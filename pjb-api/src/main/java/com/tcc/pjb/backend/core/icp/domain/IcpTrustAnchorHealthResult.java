package com.tcc.pjb.backend.core.icp.domain;
public record IcpTrustAnchorHealthResult(IcpBrasilTrustAnchorView anchor, IcpBrasilTrustAnchorAuditSnapshot audit, boolean healthy) {}
