package com.tcc.pjb.backend.service.security.ratelimit;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisRateLimiterStore implements RateLimiterStore {

    private static final DefaultRedisScript<Long> INCR_WITH_TTL;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local c = redis.call('INCR', KEYS[1])\n" +
                "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
                "return c\n"
        );
        INCR_WITH_TTL = script;
    }

    private final StringRedisTemplate redis;

    public RedisRateLimiterStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incr(String key, Duration ttl) {
        if (key == null || key.isBlank()) return 0L;
        long seconds = (ttl == null || ttl.isNegative() || ttl.isZero()) ? 60L : Math.max(1L, ttl.toSeconds());
        
        Long out = redis.execute(INCR_WITH_TTL, Collections.singletonList(key), String.valueOf(seconds));
        
        return Objects.requireNonNull(out, "Redis script returned null");
    }
}