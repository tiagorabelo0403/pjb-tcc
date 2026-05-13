package com.tcc.pjb.backend.platform.cluster;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class PjbRedisClusterLockService implements PjbClusterLockService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final MeterRegistry meterRegistry;

    public PjbRedisClusterLockService(StringRedisTemplate redis, String keyPrefix, MeterRegistry meterRegistry) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "pjb:coord:" : keyPrefix.trim();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<Lease> tryAcquire(String key, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(30) : ttl;
        String owner = UUID.randomUUID().toString();
        String namespacedKey = keyPrefix + key;
        Boolean acquired = redis.opsForValue().setIfAbsent(namespacedKey, owner, effectiveTtl);
        if (Boolean.TRUE.equals(acquired)) {
            increment("pjb.cluster.lock.acquired", "backend", "redis");
            return Optional.of(new RedisLease(namespacedKey, owner));
        }
        increment("pjb.cluster.lock.contended", "backend", "redis");
        return Optional.empty();
    }

    private void increment(String name, String tagKey, String tagValue) {
        if (meterRegistry != null) {
            meterRegistry.counter(name, tagKey, tagValue).increment();
        }
    }

    private final class RedisLease implements Lease {
        private final String key;
        private final String owner;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private RedisLease(String key, String owner) {
            this.key = key;
            this.owner = owner;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public String owner() {
            return owner;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            redis.execute(RELEASE_SCRIPT, Collections.singletonList(key), owner);
            increment("pjb.cluster.lock.released", "backend", "redis");
        }
    }
}
