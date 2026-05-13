package com.tcc.pjb.backend.service.security.ratelimit;

import java.time.Duration;

public interface RateLimiterStore {

    
    long incr(String key, Duration ttl);
}
