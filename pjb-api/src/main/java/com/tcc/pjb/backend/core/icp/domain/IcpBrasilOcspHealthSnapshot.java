package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilOcspHealthSnapshot(boolean enabled, long ttlSeconds, String cachePrefix) {}
