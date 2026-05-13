package com.tcc.pjb.backend.platform.cluster;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class PjbHybridClusterLockService implements PjbClusterLockService {

    private final PjbClusterLockService primary;
    private final PjbClusterLockService fallback;

    public PjbHybridClusterLockService(PjbClusterLockService primary,
                                       PjbClusterLockService fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public Optional<Lease> tryAcquire(String key, Duration ttl) {
        try {
            return primary.tryAcquire(key, ttl);
        } catch (RuntimeException ex) {
            return fallback.tryAcquire(key, ttl);
        }
    }
}
