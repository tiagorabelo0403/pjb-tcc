package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRuntimeSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalOfficialSourceConnectorRuntimeStateRepository {

    private static final int MAX_ENTRIES = 2048;
    private static final int TARGET_ENTRIES = 1536;
    private static final Duration FALLBACK_TTL = Duration.ofHours(12);

    private final ConcurrentHashMap<String, InstitutionalOfficialSourceConnectorRuntimeSnapshot> store = new ConcurrentHashMap<>();
    private final AtomicLong cleanupClock = new AtomicLong();

    public Optional<InstitutionalOfficialSourceConnectorRuntimeSnapshot> findActive(String sourceCode, Instant now) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return Optional.empty();
        }
        maybeCleanup(now);
        String normalizedSource = normalize(sourceCode);
        InstitutionalOfficialSourceConnectorRuntimeSnapshot snapshot = store.get(normalizedSource);
        if (snapshot == null) {
            return Optional.empty();
        }
        if (isExpired(snapshot, now)) {
            store.remove(normalizedSource, snapshot);
            return Optional.empty();
        }
        if (snapshot.nextCheckAt() != null && now != null && !snapshot.nextCheckAt().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public InstitutionalOfficialSourceConnectorRuntimeSnapshot save(InstitutionalOfficialSourceConnectorRuntimeSnapshot snapshot) {
        Instant now = Instant.now();
        maybeCleanup(now);
        store.put(normalize(snapshot.sourceCode()), snapshot);
        if (store.size() > MAX_ENTRIES) {
            trimOverflow();
        }
        return snapshot;
    }

    private void maybeCleanup(Instant now) {
        long tick = cleanupClock.incrementAndGet();
        if ((tick & 255L) != 0L && store.size() <= MAX_ENTRIES) {
            return;
        }
        Instant reference = now == null ? Instant.now() : now;
        store.entrySet().removeIf(entry -> isExpired(entry.getValue(), reference));
        if (store.size() > MAX_ENTRIES) {
            trimOverflow();
        }
    }

    private void trimOverflow() {
        int overflow = store.size() - TARGET_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        store.entrySet().stream()
                .sorted(Comparator.comparing(entry -> expiryReference(entry.getValue())))
                .limit(overflow)
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(store::remove);
    }

    private boolean isExpired(InstitutionalOfficialSourceConnectorRuntimeSnapshot snapshot, Instant now) {
        return !expiryReference(snapshot).isAfter(now == null ? Instant.now() : now);
    }

    private Instant expiryReference(InstitutionalOfficialSourceConnectorRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return Instant.EPOCH;
        }
        if (snapshot.nextCheckAt() != null) {
            return snapshot.nextCheckAt();
        }
        if (snapshot.checkedAt() != null) {
            return snapshot.checkedAt().plus(FALLBACK_TTL);
        }
        return Instant.EPOCH;
    }

    private static String normalize(String sourceCode) {
        return sourceCode == null ? "" : sourceCode.trim().toUpperCase(Locale.ROOT);
    }
}
