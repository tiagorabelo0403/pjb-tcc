package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilChainAuditSnapshot(IcpBrasilValidationProjection validation, IcpBrasilChainValidationDetails details) {
    public String failureReason() { return validation == null ? null : validation.failureReason(); }
}
