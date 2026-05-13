package com.tcc.pjb.backend.configs.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

public final class PjbBoundedLocalCacheManager implements CacheManager {

    private final PjbCacheProperties properties;
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();

    public PjbBoundedLocalCacheManager(PjbCacheProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public Cache getCache(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return caches.computeIfAbsent(name, this::buildCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return List.copyOf(caches.keySet());
    }

    private Cache buildCache(String cacheName) {
        Duration ttl = sanitizeTtl(properties.resolveTtl(cacheName));
        long maximumSize = Math.max(1L, properties.resolveMaximumSize(cacheName));
        return new CaffeineCache(cacheName, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .build(), false);
    }

    private static Duration sanitizeTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return Duration.ofMinutes(15);
        }
        return ttl;
    }
}
