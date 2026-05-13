package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilCertificateSnapshot(String subjectDn,
                                           String issuerDn,
                                           String serialHex,
                                           String acSigla) {
}
