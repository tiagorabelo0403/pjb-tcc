package com.tcc.pjb.backend.core.icp.domain;

import java.util.List;

public record IcpAcceptedAcView(
        List<String> acceptedAcSiglas,
        boolean enforceChainValidation,
        String ocspCacheKeyPrefix
) {}
