package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLiveClusterStateStore implements LiveClusterStateStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisLiveClusterStateStore(StringRedisTemplate redis,
                                      ObjectMapper objectMapper,
                                      @Value("${pjb.live.cluster.key-prefix:pjb:live:cluster:}") String configuredKeyPrefix) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        String prefix = configuredKeyPrefix == null || configuredKeyPrefix.isBlank() ? "pjb:live:cluster:" : configuredKeyPrefix.trim();
        this.keyPrefix = prefix.endsWith(":") ? prefix : prefix + ':';
    }

    @Override
    public long nextSequence(String namespace, String topic) {
        Long next = redis.opsForValue().increment(sequenceKey(namespace, topic));
        return next == null ? 0L : next;
    }

    @Override
    public void appendEvent(String namespace, String topic, long sequence, String payload, int replayBufferSize, Duration ttl) {
        if (isBlank(namespace) || isBlank(topic) || isBlank(payload) || sequence <= 0) {
            return;
        }
        String replayKey = replayKey(namespace, topic);
        try {
            redis.opsForList().rightPush(replayKey, objectMapper.writeValueAsString(new ReplayEnvelope(sequence, payload, Instant.now())));
            redis.opsForList().trim(replayKey, -Math.max(10, replayBufferSize), -1);
            expire(replayKey, ttl);
            redis.opsForSet().add(namespaceTopicsKey(namespace), topic.trim());
            expire(sequenceKey(namespace, topic), ttl);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<ReplayEntry> replayAfter(String namespace, String topic, long afterSequence, int limit) {
        String replayKey = replayKey(namespace, topic);
        List<String> raw = redis.opsForList().range(replayKey, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        int safeLimit = limit <= 0 ? raw.size() : limit;
        List<ReplayEntry> out = new ArrayList<>();
        for (String item : raw) {
            if (item == null || item.isBlank()) {
                continue;
            }
            try {
                ReplayEnvelope envelope = objectMapper.readValue(item, ReplayEnvelope.class);
                if (envelope.sequence() > afterSequence && envelope.payload() != null && !envelope.payload().isBlank()) {
                    out.add(new ReplayEntry(envelope.sequence(), envelope.payload(), envelope.createdAt()));
                }
            } catch (Exception ignored) {
            }
        }
        if (out.size() <= safeLimit) {
            return out;
        }
        return out.subList(Math.max(0, out.size() - safeLimit), out.size());
    }

    @Override
    public long latestSequence(String namespace, String topic) {
        String value = redis.opsForValue().get(sequenceKey(namespace, topic));
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    @Override
    public void syncSubscriberCount(String namespace, String topic, long count, Duration ttl) {
        if (isBlank(namespace) || isBlank(topic)) {
            return;
        }
        String countKey = subscriberKey(namespace, topic);
        String topicsKey = namespaceTopicsKey(namespace);
        if (count <= 0) {
            redis.delete(countKey);
            redis.opsForSet().remove(topicsKey, topic.trim());
            return;
        }
        redis.opsForValue().set(countKey, Long.toString(count));
        expire(countKey, ttl);
        redis.opsForSet().add(topicsKey, topic.trim());
        expire(topicsKey, ttl.multipliedBy(4));
    }

    @Override
    public long subscriberCount(String namespace, String topic) {
        String value = redis.opsForValue().get(subscriberKey(namespace, topic));
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    @Override
    public long totalSubscribers(String namespace) {
        long total = 0L;
        for (String topic : activeTopicNames(namespace)) {
            total += subscriberCount(namespace, topic);
        }
        return total;
    }

    @Override
    public long activeTopics(String namespace) {
        return activeTopicNames(namespace).size();
    }

    @Override
    public Map<String, Long> topicSubscriberSnapshot(String namespace, int limit) {
        int safeLimit = limit <= 0 ? 25 : limit;
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        activeTopicNames(namespace).stream()
            .map(topic -> Map.entry(topic, subscriberCount(namespace, topic)))
            .filter(entry -> entry.getValue() > 0)
            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed())
            .limit(safeLimit)
            .forEach(entry -> out.put(entry.getKey(), entry.getValue()));
        return out;
    }

    @Override
    public boolean distributed() {
        return true;
    }

    private List<String> activeTopicNames(String namespace) {
        Set<String> members = redis.opsForSet().members(namespaceTopicsKey(namespace));
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> live = new LinkedHashSet<>();
        for (String topic : members) {
            if (topic == null || topic.isBlank()) {
                continue;
            }
            long count = subscriberCount(namespace, topic);
            if (count > 0) {
                live.add(topic);
            } else {
                redis.opsForSet().remove(namespaceTopicsKey(namespace), topic);
            }
        }
        return new ArrayList<>(live);
    }

    private void expire(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redis.expire(key, ttl);
    }

    private String sequenceKey(String namespace, String topic) {
        return keyPrefix + "seq:" + normalize(namespace) + ':' + normalize(topic);
    }

    private String replayKey(String namespace, String topic) {
        return keyPrefix + "replay:" + normalize(namespace) + ':' + normalize(topic);
    }

    private String subscriberKey(String namespace, String topic) {
        return keyPrefix + "subs:" + normalize(namespace) + ':' + normalize(topic);
    }

    private String namespaceTopicsKey(String namespace) {
        return keyPrefix + "topics:" + normalize(namespace);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private record ReplayEnvelope(long sequence, String payload, Instant createdAt) {
    }
}
