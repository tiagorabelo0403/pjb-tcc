package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(name = "pjb.live.cluster.enabled", havingValue = "true")
public class RedisLiveClusterBus implements LiveClusterBus, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisLiveClusterBus.class);
    private static final int MAX_NAMESPACES = 64;
    private static final int MAX_HANDLERS_PER_NAMESPACE = 32;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String channelPrefix;
    private final String nodeId;
    private final Map<String, CopyOnWriteArrayList<Consumer<LiveClusterEvent>>> handlers = new ConcurrentHashMap<>();

    public RedisLiveClusterBus(StringRedisTemplate redis,
                               ObjectMapper objectMapper,
                               @Value("${pjb.live.cluster.channel-prefix:pjb:live:}") String channelPrefix,
                               @Value("${pjb.live.cluster.node-id:}") String configuredNodeId) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        String normalizedPrefix = channelPrefix == null || channelPrefix.isBlank() ? "pjb:live:" : channelPrefix.trim();
        this.channelPrefix = normalizedPrefix.endsWith(":") ? normalizedPrefix : normalizedPrefix + ':';
        String candidateNodeId = configuredNodeId == null ? "" : configuredNodeId.trim();
        if (candidateNodeId.isBlank()) {
            String host = System.getenv("HOSTNAME");
            candidateNodeId = host != null && !host.isBlank() ? host.trim() : UUID.randomUUID().toString();
        }
        this.nodeId = candidateNodeId;
    }

    @Override
    public void publish(String namespace, LiveClusterEvent event) {
        if (isBlank(namespace) || event == null || isBlank(event.topic()) || isBlank(event.payload()) || event.sequence() <= 0) {
            return;
        }
        ClusterEnvelope envelope = new ClusterEnvelope(nodeId, normalize(namespace), event.topic().trim(), event.sequence(), event.payload(), event.createdAt() == null ? Instant.now() : event.createdAt());
        try {
            redis.convertAndSend(channel(normalize(namespace)), objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            log.warn("live cluster publish failed namespace={} topic={} reason={}", namespace, event.topic(), ex.getMessage());
        }
    }

    @Override
    public void registerHandler(String namespace, Consumer<LiveClusterEvent> handler) {
        if (isBlank(namespace) || handler == null) {
            return;
        }
        String normalizedNamespace = normalize(namespace);
        CopyOnWriteArrayList<Consumer<LiveClusterEvent>> existing = handlers.get(normalizedNamespace);
        if (existing == null && handlers.size() >= MAX_NAMESPACES) {
            log.warn("live cluster handler namespace rejected namespace={} reason=max_namespaces", normalizedNamespace);
            return;
        }
        CopyOnWriteArrayList<Consumer<LiveClusterEvent>> consumers = handlers.computeIfAbsent(normalizedNamespace, key -> new CopyOnWriteArrayList<>());
        if (consumers.contains(handler)) {
            return;
        }
        if (consumers.size() >= MAX_HANDLERS_PER_NAMESPACE) {
            log.warn("live cluster handler rejected namespace={} reason=max_handlers", normalizedNamespace);
            return;
        }
        consumers.add(handler);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        try {
            ClusterEnvelope envelope = objectMapper.readValue(message.getBody(), ClusterEnvelope.class);
            if (envelope == null || isBlank(envelope.namespace()) || isBlank(envelope.topic()) || isBlank(envelope.payload()) || envelope.sequence() <= 0) {
                return;
            }
            if (nodeId.equals(envelope.nodeId())) {
                return;
            }
            List<Consumer<LiveClusterEvent>> consumers = handlers.get(normalize(envelope.namespace()));
            if (consumers == null || consumers.isEmpty()) {
                return;
            }
            LiveClusterEvent event = new LiveClusterEvent(envelope.topic(), envelope.sequence(), envelope.payload(), envelope.createdAt());
            for (Consumer<LiveClusterEvent> consumer : consumers) {
                try {
                    consumer.accept(event);
                } catch (Exception ex) {
                    log.debug("live cluster consumer failed namespace={} topic={} reason={}", envelope.namespace(), envelope.topic(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            String channel = message.getChannel() == null ? "unknown" : new String(message.getChannel(), StandardCharsets.UTF_8);
            log.debug("live cluster message ignored channel={} reason={}", channel, ex.getMessage());
        }
    }

    public String pattern() {
        return channelPrefix + '*';
    }

    private String channel(String namespace) {
        return channelPrefix + namespace;
    }

    private static String normalize(String namespace) {
        return namespace.trim().toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ClusterEnvelope(String nodeId,
                                   String namespace,
                                   String topic,
                                   long sequence,
                                   String payload,
                                   Instant createdAt) {
    }
}
