package com.tcc.pjb.backend.core.icp;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IcpBrasilCrlCache {

    private final IcpBrasilSignatureProperties properties;
    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public IcpBrasilCrlCache(IcpBrasilSignatureProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public Optional<byte[]> find(String distributionPoint) {
        String key = normalize(distributionPoint);
        if (key == null) {
            return Optional.empty();
        }
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.payloadCopy());
    }

    public void put(String distributionPoint, byte[] crlBytes) {
        String key = normalize(distributionPoint);
        if (key == null || crlBytes == null || crlBytes.length == 0) {
            return;
        }
        long ttlSeconds = Math.max(60L, properties.crlCacheTtlSeconds());
        entries.put(key, new CacheEntry(Arrays.copyOf(crlBytes, crlBytes.length), Instant.now().plusSeconds(ttlSeconds)));
    }

    public void evictExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public int size() {
        evictExpired();
        return entries.size();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }

    private record CacheEntry(byte[] payload, Instant expiresAt) {
        private byte[] payloadCopy() {
            return Arrays.copyOf(payload, payload.length);
        }
    }
}
