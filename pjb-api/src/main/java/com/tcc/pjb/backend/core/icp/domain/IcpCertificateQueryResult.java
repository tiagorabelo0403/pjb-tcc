package com.tcc.pjb.backend.core.icp.domain;

public record IcpCertificateQueryResult(IcpBrasilCertificateView cache,
                                        IcpBrasilCertificateAuditSnapshot audit) {}
