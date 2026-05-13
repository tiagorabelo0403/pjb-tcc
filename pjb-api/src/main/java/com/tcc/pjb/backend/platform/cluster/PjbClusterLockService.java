package com.tcc.pjb.backend.platform.cluster;

import java.time.Duration;
import java.util.Optional;

public interface PjbClusterLockService {

    Optional<Lease> tryAcquire(String key, Duration ttl);

    interface Lease extends AutoCloseable {
        String key();
        String owner();
        @Override
        void close();
    }
}
