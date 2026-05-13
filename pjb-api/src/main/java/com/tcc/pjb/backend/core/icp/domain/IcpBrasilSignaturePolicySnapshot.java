package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilSignaturePolicySnapshot(boolean enabled,
                                               boolean enforceChainValidation,
                                               String profileCandidate) {
}
