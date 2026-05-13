package com.tcc.pjb.backend.service.security.blocklist;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisBlocklistStore implements BlocklistStore {

    private final StringRedisTemplate redis;
    private final String keyPrefix;

    public RedisBlocklistStore(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "pjb:sec:block:" : keyPrefix;
    }

    @Override
    public void banIp(String ip, String reason, Duration ttl) {
        if (ip == null || ip.isBlank()) return;
        Duration effective = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofHours(24) : ttl;
        String key = keyPrefix + ip;
        String value = (reason == null || reason.isBlank()) ? "blocked" : reason;

        redis.opsForValue().set(key, value, effective);
        log.warn("[BLOCKLIST:REDIS] ip={} ttl={} reason={}", ip, effective, value);
    }

    @Override
    public Optional<String> getReason(String ip) {
        if (ip == null || ip.isBlank()) return Optional.empty();
        String value = redis.opsForValue().get(keyPrefix + ip);
        if (value == null || value.isBlank()) return Optional.empty();
        return Optional.of(value);
    }

    @Override
    public void unbanIp(String ip) {
        if (ip == null || ip.isBlank()) return;
        redis.delete(keyPrefix + ip);
    }
}
