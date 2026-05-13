package com.tcc.pjb.backend.configs.live;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LiveClusterStateStore {

    long nextSequence(String namespace, String topic);

    void appendEvent(String namespace, String topic, long sequence, String payload, int replayBufferSize, Duration ttl);

    List<ReplayEntry> replayAfter(String namespace, String topic, long afterSequence, int limit);

    long latestSequence(String namespace, String topic);

    void syncSubscriberCount(String namespace, String topic, long count, Duration ttl);

    long subscriberCount(String namespace, String topic);

    long totalSubscribers(String namespace);

    long activeTopics(String namespace);

    Map<String, Long> topicSubscriberSnapshot(String namespace, int limit);

    boolean distributed();

    record ReplayEntry(long sequence, String payload, Instant createdAt) {
    }
}
