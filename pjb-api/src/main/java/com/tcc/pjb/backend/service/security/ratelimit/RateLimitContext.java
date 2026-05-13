package com.tcc.pjb.backend.service.security.ratelimit;


public record RateLimitContext(
        String clientIp,
        String httpMethod,
        String path,
        String userKey
) {
}
