package com.tcc.pjb.backend.core.icp.domain;

public record IcpAssinaturaMetricsView(
        boolean validationOk,
        boolean ocspOk,
        boolean chainOk,
        boolean signed
) {}
