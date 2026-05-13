package com.tcc.pjb.backend.platform.security.ratelimit;


public record CapabilityRateLimitDecision(
        boolean allowed,
        long limitTokens,
        long remainingTokens,
        long retryAfterSeconds,
        int windowSeconds,
        int costTokens
) {
}
