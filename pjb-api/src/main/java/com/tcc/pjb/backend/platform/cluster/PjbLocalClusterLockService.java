package com.tcc.pjb.backend.platform.cluster;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PjbLocalClusterLockService implements PjbClusterLockService {

    private static final int MAX_LOCKS = 50000;

    private final String keyPrefix;
    private final ConcurrentHashMap<String, LocalEntry> locks = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public PjbLocalClusterLockService(String keyPrefix, MeterRegistry meterRegistry) {
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<Lease> tryAcquire(String key, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Duration effectiveTtl = normalizeTtl(ttl);
        cleanupExpired(System.nanoTime(), false);
        String namespacedKey = keyPrefix + key;
        String owner = UUID.randomUUID().toString();
        for (;;) {
            long now = System.nanoTime();
            long expiresAt = now + effectiveTtl.toNanos();
            LocalEntry current = locks.get(namespacedKey);
            if (current != null && current.expiresAtNanos() > now) {
                increment("pjb.cluster.lock.contended", "backend", "local");
                return Optional.empty();
            }
            if (current != null && !locks.remove(namespacedKey, current)) {
                continue;
            }
            LocalEntry candidate = new LocalEntry(owner, expiresAt);
            if (locks.putIfAbsent(namespacedKey, candidate) == null) {
                cleanupExpired(now, locks.size() > MAX_LOCKS);
                increment("pjb.cluster.lock.acquired", "backend", "local");
                return Optional.of(new LocalLease(namespacedKey, owner));
            }
        }
    }

    private void cleanupExpired(long now, boolean forceTrim) {
        locks.entrySet().removeIf(entry -> {
            LocalEntry value = entry.getValue();
            return value == null || value.expiresAtNanos() <= now;
        });
        if (!forceTrim) {
            return;
        }
        int overflow = locks.size() - MAX_LOCKS;
        if (overflow <= 0) {
            return;
        }
        var iterator = locks.entrySet().iterator();
        while (overflow > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            overflow--;
        }
    }

    private void increment(String name, String tagKey, String tagValue) {
        if (meterRegistry != null) {
            meterRegistry.counter(name, tagKey, tagValue).increment();
        }
    }

    private static String normalizePrefix(String keyPrefix) {
        return keyPrefix == null || keyPrefix.isBlank() ? "pjb:coord:" : keyPrefix.trim();
    }

    private static Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return Duration.ofSeconds(30);
        }
        return ttl;
    }

    private record LocalEntry(String owner, long expiresAtNanos) {
    }

    private final class LocalLease implements Lease {
        private final String key;
        private final String owner;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private LocalLease(String key, String owner) {
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
            locks.computeIfPresent(key, (ignored, current) -> owner.equals(current.owner()) ? null : current);
            increment("pjb.cluster.lock.released", "backend", "local");
        }
    }
}
