package com.tcc.pjb.backend.core.icp.domain;

public record IcpOcspCacheView(
        String cacheKeyPrefix,
        long ttlSeconds,
        boolean enabled,
        String scope
) {}
