package com.tcc.pjb.backend.platform.security.ratelimit;


public interface CapabilityRateLimitStore {

    CapabilityRateLimitDecision tryConsume(String key,
                                          long nowEpochSecond,
                                          int windowSeconds,
                                          int limitTokens,
                                          int costTokens);
}
