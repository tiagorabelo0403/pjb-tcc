package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilSignatureAuditSnapshot(String docHash,
                                              String serialHex,
                                              String profileCandidate,
                                              String profileAchieved,
                                              boolean validationOk) {}
