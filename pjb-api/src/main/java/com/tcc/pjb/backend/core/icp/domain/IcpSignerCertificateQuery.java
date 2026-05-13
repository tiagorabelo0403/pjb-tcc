package com.tcc.pjb.backend.core.icp.domain;

public record IcpSignerCertificateQuery(
        Long processoId,
        String profileCandidate,
        boolean requireCertificate
) {}
