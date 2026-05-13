package com.tcc.pjb.backend.service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxPublisher {

    public static final String EVT_SECRETARIAT_LIVE = "SECRETARIAT_LIVE";
    public static final String EVT_UI_HISTORY_LIVE = "UI_HISTORY_LIVE";
    public static final String EVT_UI_ACCESSIBILITY_LIVE = "UI_ACCESSIBILITY_LIVE";
    public static final String EVT_UI_PRESENTATION_LIVE = "UI_PRESENTATION_LIVE";
    public static final String EVT_CIDADAO_DASHBOARD_REFRESH = "CIDADAO_DASHBOARD_REFRESH";
    public static final String EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH = "PROFILE_INSTITUTIONAL_MESH_DISPATCH";

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final List<String> SENSITIVE_TOKENS = List.of(
            "authorization", "password", "senha", "secret", "token", "cookie", "session", "apiKey", "api_key", "credential"
    );

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxIngressProperties properties;
    private final BlockingQueue<PersistableOutboxMessage> ingressQueue;
    private final ConcurrentLinkedDeque<OutboxMessage> messages = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<LiveMessage> liveMessages = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private final AtomicLong directPersists = new AtomicLong();
    private final AtomicLong queuedPersists = new AtomicLong();
    private final AtomicLong rejectedPersists = new AtomicLong();
    private final ExecutorService flushExecutor;

    public OutboxPublisher(ObjectMapper objectMapper,
                           OutboxEventRepository repository,
                           PlatformTransactionManager transactionManager,
                           OutboxIngressProperties properties,
                           @Qualifier("pjbJobExecutorService") ExecutorService flushExecutor) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transactionTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        this.properties = Objects.requireNonNull(properties, "properties");
        this.ingressQueue = new ArrayBlockingQueue<>(Math.max(256, properties.getCapacity()));
        this.flushExecutor = Objects.requireNonNull(flushExecutor, "flushExecutor");
    }

    public void enqueue(String topic,
                        String eventCode,
                        Object payload,
                        Map<String, ?> metadata,
                        String deduplicationKey,
                        String aggregateType,
                        String aggregateId) {
        PersistableOutboxMessage command = toPersistable(topic, eventCode, payload, metadata, deduplicationKey, aggregateType, aggregateId);
        recordMessage(command.toMessage());
        if (!properties.isEnabled()) {
            persistDirect(command);
            return;
        }
        boolean offered = offer(command);
        if (offered) {
            queuedPersists.incrementAndGet();
            if (ingressQueue.size() >= Math.max(1, properties.getFlushBatchSize())) {
                triggerAsyncFlush();
            }
            return;
        }
        rejectedPersists.incrementAndGet();
        if (properties.isFailFastWhenSaturated() && !properties.isFallbackToDirectPersist()) {
            throw new RejectedExecutionException("outbox ingress saturated");
        }
        persistDirect(command);
    }

    public UUID enqueueTracked(String topic,
                               String eventCode,
                               Object payload,
                               Map<String, ?> metadata,
                               String deduplicationKey,
                               String aggregateType,
                               String aggregateId) {
        PersistableOutboxMessage command = toPersistable(topic, eventCode, payload, metadata, deduplicationKey, aggregateType, aggregateId);
        recordMessage(command.toMessage());
        if (!properties.isEnabled()) {
            persistDirect(command);
            return command.id();
        }
        boolean offered = offer(command);
        if (offered) {
            queuedPersists.incrementAndGet();
            if (ingressQueue.size() >= Math.max(1, properties.getFlushBatchSize())) {
                triggerAsyncFlush();
            }
            return command.id();
        }
        rejectedPersists.incrementAndGet();
        if (properties.isFailFastWhenSaturated() && !properties.isFallbackToDirectPersist()) {
            throw new RejectedExecutionException("outbox ingress saturated");
        }
        persistDirect(command);
        return command.id();
    }

    public void enqueueSecretariatLive(String inboxKey, String eventCode, Instant happenedAt, Object payload) {
        Instant resolved = happenedAt == null ? Instant.now() : happenedAt;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inboxKey", normalizeText(inboxKey, 180));
        body.put("eventCode", normalizeText(eventCode, 120));
        body.put("happenedAt", resolved);
        body.put("payload", payload);
        recordLiveMessage(new LiveMessage(inboxKey, eventCode, resolved, payload));
        enqueue(inboxKey, EVT_SECRETARIAT_LIVE, body, Map.of("channel", "secretariat-live"), liveDedup(inboxKey, eventCode, resolved, payload), "SECRETARIAT_LIVE", inboxKey);
    }

    @Scheduled(fixedDelayString = "${pjb.outbox.ingress.flush-interval:100ms}")
    public void flushIngress() {
        drainAndPersist(false);
    }

    @PreDestroy
    public void shutdown() {
        drainAndPersist(true);
        flushExecutor.shutdown();
        try {
            if (!flushExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                flushExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            flushExecutor.shutdownNow();
            log.warn("outbox flush executor interrupted during shutdown", ex);
            Thread.currentThread().interrupt();
        }
    }

    public List<OutboxMessage> messages() {
        return List.copyOf(messages);
    }

    public List<LiveMessage> liveMessages() {
        return List.copyOf(liveMessages);
    }

    public int ingressDepth() {
        return ingressQueue.size();
    }

    public long directPersistCount() {
        return directPersists.get();
    }

    public long queuedPersistCount() {
        return queuedPersists.get();
    }

    public long rejectedPersistCount() {
        return rejectedPersists.get();
    }

    private boolean offer(PersistableOutboxMessage command) {
        try {
            long timeoutMs = Math.max(0L, properties.getOfferTimeout().toMillis());
            if (timeoutMs == 0L) {
                return ingressQueue.offer(command);
            }
            return ingressQueue.offer(command, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void triggerAsyncFlush() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }
        flushExecutor.execute(() -> drainAndPersist(true));
    }

    private void drainAndPersist(boolean greedy) {
        if (!flushing.compareAndSet(false, true) && !greedy) {
            return;
        }
        try {
            int burstLimit = greedy ? Integer.MAX_VALUE : Math.max(1, properties.getFlushBurstLimit());
            int batchSize = Math.max(1, properties.getFlushBatchSize());
            int bursts = 0;
            while (!ingressQueue.isEmpty() && bursts < burstLimit) {
                List<PersistableOutboxMessage> batch = new java.util.ArrayList<>(batchSize);
                ingressQueue.drainTo(batch, batchSize);
                if (batch.isEmpty()) {
                    break;
                }
                persistBatch(batch);
                bursts++;
            }
        } finally {
            flushing.set(false);
            if (!ingressQueue.isEmpty()) {
                triggerAsyncFlush();
            }
        }
    }

    private void persistBatch(List<PersistableOutboxMessage> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> repository.saveAll(batch.stream().map(PersistableOutboxMessage::toEntity).toList()));
    }

    private void persistDirect(PersistableOutboxMessage command) {
        directPersists.incrementAndGet();
        transactionTemplate.executeWithoutResult(status -> repository.save(command.toEntity()));
    }

    private PersistableOutboxMessage toPersistable(String topic,
                                                   String eventCode,
                                                   Object payload,
                                                   Map<String, ?> metadata,
                                                   String deduplicationKey,
                                                   String aggregateType,
                                                   String aggregateId) {
        Instant createdAt = Instant.now();
        String routingKey = normalizeText(topic, 180);
        String type = normalizeText(eventCode, 120);
        JsonNode payloadNode = sanitizeNode(objectMapper.valueToTree(payload), null, 0);
        JsonNode headersNode = sanitizeNode(toMetadataNode(metadata), null, 0);
        String payloadJson = toSizedJson(payloadNode, properties.getMaxPayloadBytes(), payload == null ? "null" : payload.getClass().getName());
        String headersJson = toSizedJson(headersNode, properties.getMaxHeadersBytes(), "headers");
        String dedupKey = normalizeDedupKey(deduplicationKey, routingKey, type, payloadJson);
        String safeAggregateType = blankToNull(normalizeText(aggregateType, 120));
        String safeAggregateId = blankToNull(normalizeText(aggregateId, 180));
        return new PersistableOutboxMessage(UUID.randomUUID(), routingKey, type, payloadJson, headersJson, dedupKey, safeAggregateType, safeAggregateId, createdAt);
    }

    private JsonNode toMetadataNode(Map<String, ?> metadata) {
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        if (metadata != null && !metadata.isEmpty()) {
            int limit = Math.max(1, properties.getMaxMetadataEntries());
            int count = 0;
            for (Map.Entry<String, ?> entry : metadata.entrySet()) {
                if (entry == null || entry.getKey() == null || count >= limit) {
                    continue;
                }
                sanitized.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        return objectMapper.valueToTree(sanitized);
    }

    private JsonNode sanitizeNode(JsonNode node, String fieldName, int depth) {
        if (node == null || node.isNull()) {
            return objectMapper.nullNode();
        }
        if (depth > 10) {
            return objectMapper.getNodeFactory().textNode("[TRUNCATED_DEPTH]");
        }
        if (isSensitive(fieldName)) {
            return objectMapper.getNodeFactory().textNode("[REDACTED]");
        }
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            node.fieldNames().forEachRemaining(childFieldName -> objectNode.set(childFieldName, sanitizeNode(node.get(childFieldName), childFieldName, depth + 1)));
            return objectNode;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            int max = Math.min(node.size(), 256);
            for (int i = 0; i < max; i++) {
                arrayNode.add(sanitizeNode(node.get(i), fieldName, depth + 1));
            }
            return arrayNode;
        }
        if (node.isBinary()) {
            return objectMapper.getNodeFactory().textNode("[BINARY]");
        }
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(limitString(node.asText()));
        }
        return node;
    }

    private String toSizedJson(JsonNode node, int maxBytes, String typeHint) {
        try {
            String json = objectMapper.writeValueAsString(node);
            int limit = Math.max(4096, maxBytes);
            if (json.getBytes(StandardCharsets.UTF_8).length <= limit) {
                return json;
            }
            String hash = Hashes.sha256Hex(json);
            Map<String, Object> compact = Map.of(
                    "truncated", true,
                    "payloadHash", hash,
                    "payloadType", typeHint,
                    "sizeBytes", json.getBytes(StandardCharsets.UTF_8).length
            );
            return objectMapper.writeValueAsString(compact);
        } catch (Exception ex) {
            throw new IllegalStateException("outbox serialization failure", ex);
        }
    }

    private String normalizeDedupKey(String deduplicationKey, String routingKey, String eventType, String payloadJson) {
        String normalized = blankToNull(normalizeText(deduplicationKey, 200));
        if (normalized != null) {
            return normalized;
        }
        String base = routingKey + '|' + eventType + '|' + Hashes.sha256Hex(payloadJson);
        return "auto:" + Hashes.sha256HexPrefix(base, 48);
    }

    private String liveDedup(String inboxKey, String eventCode, Instant happenedAt, Object payload) {
        String payloadText = payload == null ? "null" : payload.toString();
        String basis = inboxKey + '|' + eventCode + '|' + happenedAt + '|' + Hashes.sha256Hex(payloadText);
        return "live:" + Hashes.sha256HexPrefix(basis, 48);
    }

    private void recordMessage(OutboxMessage message) {
        messages.addLast(message);
        trim(messages);
    }

    private void recordLiveMessage(LiveMessage message) {
        liveMessages.addLast(message);
        trim(liveMessages);
    }

    private <T> void trim(ConcurrentLinkedDeque<T> deque) {
        int limit = Math.max(128, properties.getHistoryLimit());
        while (deque.size() > limit) {
            deque.pollFirst();
        }
    }

    private boolean isSensitive(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String lowered = fieldName.toLowerCase(java.util.Locale.ROOT);
        for (String token : SENSITIVE_TOKENS) {
            if (lowered.contains(token.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String limitString(String value) {
        if (value == null) {
            return null;
        }
        int max = Math.max(256, properties.getMaxStringLength());
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String normalizeText(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record OutboxMessage(String topic,
                                String eventCode,
                                Object payload,
                                Map<String, ?> metadata,
                                String deduplicationKey,
                                String aggregateType,
                                String aggregateId,
                                Instant createdAt) {
    }

    public record LiveMessage(String inboxKey, String eventCode, Instant happenedAt, Object payload) {
    }

    private record PersistableOutboxMessage(UUID id,
                                            String routingKey,
                                            String eventType,
                                            String payloadJson,
                                            String headersJson,
                                            String dedupKey,
                                            String aggregateType,
                                            String aggregateId,
                                            Instant createdAt) {

        OutboxEvent toEntity() {
            return new OutboxEvent(id, routingKey, eventType, payloadJson, createdAt, dedupKey, aggregateType, aggregateId, headersJson);
        }

        OutboxMessage toMessage() {
            return new OutboxMessage(routingKey, eventType, payloadJson, Map.of("headersJson", headersJson), dedupKey, aggregateType, aggregateId, createdAt);
        }
    }
}
