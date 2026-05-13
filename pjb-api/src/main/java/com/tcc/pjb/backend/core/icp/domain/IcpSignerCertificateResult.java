package com.tcc.pjb.backend.core.icp.domain;

public record IcpSignerCertificateResult(
        Long processoId,
        String serialHex,
        String subjectDn,
        boolean available,
        String summary
) {}
