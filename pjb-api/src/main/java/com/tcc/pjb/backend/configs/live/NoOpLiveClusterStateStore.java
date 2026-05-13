package com.tcc.pjb.backend.configs.live;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NoOpLiveClusterStateStore implements LiveClusterStateStore {

    private static final Duration DEFAULT_TOPIC_TTL = Duration.ofMinutes(15);
    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();

    private final ConcurrentHashMap<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<ReplayEntry>> replay = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> subscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> topicExpiryEpochMillis = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupEpochMillis = new AtomicLong(System.currentTimeMillis() + CLEANUP_INTERVAL_MILLIS);

    @Override
    public long nextSequence(String namespace, String topic) {
        cleanupIfRequired();
        String key = key(namespace, topic);
        touch(key, DEFAULT_TOPIC_TTL);
        return sequences.computeIfAbsent(key, ignored -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public void appendEvent(String namespace, String topic, long sequence, String payload, int replayBufferSize, Duration ttl) {
        if (isBlank(namespace) || isBlank(topic) || isBlank(payload) || sequence <= 0) {
            return;
        }
        cleanupIfRequired();
        String key = key(namespace, topic);
        touch(key, ttl);
        Deque<ReplayEntry> deque = replay.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(new ReplayEntry(sequence, payload, Instant.now()));
            while (deque.size() > Math.max(10, replayBufferSize)) {
                deque.removeFirst();
            }
        }
    }

    @Override
    public List<ReplayEntry> replayAfter(String namespace, String topic, long afterSequence, int limit) {
        cleanupIfRequired();
        String key = key(namespace, topic);
        if (isExpired(key)) {
            pruneKey(key);
            return List.of();
        }
        Deque<ReplayEntry> deque = replay.get(key);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }
        List<ReplayEntry> out = new ArrayList<>();
        synchronized (deque) {
            for (ReplayEntry entry : deque) {
                if (entry.sequence() > afterSequence) {
                    out.add(entry);
                }
            }
        }
        if (out.size() <= limit || limit <= 0) {
            return out;
        }
        return out.subList(Math.max(0, out.size() - limit), out.size());
    }

    @Override
    public long latestSequence(String namespace, String topic) {
        cleanupIfRequired();
        String key = key(namespace, topic);
        if (isExpired(key)) {
            pruneKey(key);
            return 0L;
        }
        AtomicLong sequence = sequences.get(key);
        return sequence == null ? 0L : sequence.get();
    }

    @Override
    public void syncSubscriberCount(String namespace, String topic, long count, Duration ttl) {
        if (isBlank(namespace) || isBlank(topic)) {
            return;
        }
        cleanupIfRequired();
        String key = key(namespace, topic);
        if (count <= 0) {
            subscribers.remove(key);
            if (!replay.containsKey(key)) {
                touch(key, Duration.ofSeconds(10));
            }
            return;
        }
        touch(key, ttl);
        subscribers.put(key, count);
    }

    @Override
    public long subscriberCount(String namespace, String topic) {
        cleanupIfRequired();
        String key = key(namespace, topic);
        if (isExpired(key)) {
            pruneKey(key);
            return 0L;
        }
        return subscribers.getOrDefault(key, 0L);
    }

    @Override
    public long totalSubscribers(String namespace) {
        cleanupIfRequired();
        String prefix = normalize(namespace) + ':';
        return subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .mapToLong(Map.Entry::getValue)
            .sum();
    }

    @Override
    public long activeTopics(String namespace) {
        cleanupIfRequired();
        String prefix = normalize(namespace) + ':';
        return subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
            .count();
    }

    @Override
    public Map<String, Long> topicSubscriberSnapshot(String namespace, int limit) {
        cleanupIfRequired();
        String prefix = normalize(namespace) + ':';
        int safeLimit = limit <= 0 ? 25 : limit;
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed())
            .limit(safeLimit)
            .forEach(entry -> out.put(entry.getKey().substring(prefix.length()), entry.getValue()));
        return out;
    }

    @Override
    public boolean distributed() {
        return false;
    }

    private void cleanupIfRequired() {
        long now = System.currentTimeMillis();
        long scheduled = nextCleanupEpochMillis.get();
        if (now < scheduled) {
            return;
        }
        if (!nextCleanupEpochMillis.compareAndSet(scheduled, now + CLEANUP_INTERVAL_MILLIS)) {
            return;
        }
        for (Iterator<Map.Entry<String, Long>> iterator = topicExpiryEpochMillis.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Long> entry = iterator.next();
            Long expiresAt = entry.getValue();
            if (expiresAt == null || expiresAt > now) {
                continue;
            }
            String key = entry.getKey();
            Long subscribersCount = subscribers.get(key);
            Deque<ReplayEntry> deque = replay.get(key);
            boolean hasSubscribers = subscribersCount != null && subscribersCount > 0;
            boolean hasReplay = deque != null && !deque.isEmpty();
            if (!hasSubscribers && !hasReplay) {
                iterator.remove();
                pruneKey(key);
            }
        }
    }

    private boolean isExpired(String key) {
        Long expiresAt = topicExpiryEpochMillis.get(key);
        return expiresAt != null && expiresAt <= System.currentTimeMillis();
    }

    private void pruneKey(String key) {
        sequences.remove(key);
        replay.remove(key);
        subscribers.remove(key);
        topicExpiryEpochMillis.remove(key);
    }

    private void touch(String key, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + sanitizeTtl(ttl).toMillis();
        topicExpiryEpochMillis.merge(key, expiresAt, Math::max);
    }

    private static Duration sanitizeTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return DEFAULT_TOPIC_TTL;
        }
        return ttl;
    }

    private static String key(String namespace, String topic) {
        return normalize(namespace) + ':' + normalize(topic);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
