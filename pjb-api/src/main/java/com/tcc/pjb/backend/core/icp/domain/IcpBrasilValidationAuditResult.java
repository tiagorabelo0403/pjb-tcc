package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilValidationAuditResult(IcpBrasilValidationResult validation,
                                             IcpBrasilChainValidationDetails details,
                                             IcpBrasilRevocationSnapshot revocation) {
    public boolean validationOk() { return validation != null && validation.valid(); }
}
