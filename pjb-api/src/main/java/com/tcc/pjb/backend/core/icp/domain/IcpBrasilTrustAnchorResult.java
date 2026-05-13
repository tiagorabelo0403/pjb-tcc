package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilTrustAnchorResult(IcpBrasilTrustAnchorView anchor, IcpBrasilTrustAnchorAuditSnapshot audit) {
    public String acSigla() { return anchor == null ? null : anchor.acSigla(); }
}
