package com.tcc.pjb.backend.service.security.ratelimit;


public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        long currentCount
) {}
