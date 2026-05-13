package com.tcc.pjb.backend.core.infra.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class MortalityCache {

    private static final Duration TTL_DECEASED = Duration.ofHours(24);
    private static final Duration TTL_ALIVE = Duration.ofMinutes(30);
    private static final int MAX_ENTRIES = 50_000;
    private static final int TARGET_ENTRIES = 40_000;

    private final Map<String, CacheEntry> byCpf = new ConcurrentHashMap<>();
    private final AtomicLong cleanupClock = new AtomicLong();

    public Optional<VitalStatus> get(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return Optional.empty();
        }
        maybeCleanup();
        String normalizedCpf = normalizeCpf(cpf);
        CacheEntry entry = byCpf.get(normalizedCpf);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            byCpf.remove(normalizedCpf, entry);
            return Optional.empty();
        }
        return Optional.of(entry.status);
    }

    public void put(String cpf, VitalStatus status) {
        if (cpf == null || cpf.isBlank() || status == null) {
            return;
        }
        maybeCleanup();
        Duration ttl = status.getState() == VitalStatus.State.DECEASED ? TTL_DECEASED : TTL_ALIVE;
        byCpf.put(normalizeCpf(cpf), CacheEntry.builder()
                .status(status)
                .expiresAt(Instant.now().plus(ttl))
                .build());
        if (byCpf.size() > MAX_ENTRIES) {
            trimOverflow();
        }
    }

    public void invalidate(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return;
        }
        byCpf.remove(normalizeCpf(cpf));
    }

    private void maybeCleanup() {
        long tick = cleanupClock.incrementAndGet();
        if ((tick & 255L) != 0L && byCpf.size() <= MAX_ENTRIES) {
            return;
        }
        purgeExpired();
        if (byCpf.size() > MAX_ENTRIES) {
            trimOverflow();
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        byCpf.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    private void trimOverflow() {
        int overflow = byCpf.size() - TARGET_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        byCpf.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().expiresAt))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(byCpf::remove);
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }


    @Value
    @Builder
    public static class VitalStatus {
        State state;
        String source;
        String reference;
        Instant checkedAt;
        Instant deathDate;

        public boolean isAlive() {
            return state == State.ALIVE;
        }

        public boolean isDeceased() {
            return state == State.DECEASED;
        }

        public boolean isUnknown() {
            return state == State.UNKNOWN;
        }

        public enum State {
            ALIVE,
            DECEASED,
            UNKNOWN
        }
    }

    @Value
    @Builder
    private static class CacheEntry {
        VitalStatus status;
        Instant expiresAt;

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
