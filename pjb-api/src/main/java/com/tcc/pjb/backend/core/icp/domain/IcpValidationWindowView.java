package com.tcc.pjb.backend.core.icp.domain;

public record IcpValidationWindowView(
        boolean enforceChainValidation,
        boolean ocspEnabled,
        boolean crlEnabled
) {}
