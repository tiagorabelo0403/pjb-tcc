package com.tcc.pjb.backend.core.icp.domain;

import java.util.List;

public record IcpSignatureQueryResult(com.tcc.pjb.backend.core.icp.domain.IcpBrasilAssinaturaSnapshot assinatura,
                                      List<IcpBrasilSignatureTimelineEntry> events,
                                      IcpBrasilSignatureAuditSnapshot audit) {}
