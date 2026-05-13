package com.tcc.pjb.backend.service.outbox;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FederatedOutboxDispatchService {

    private final OutboxPublisher outboxPublisher;
    private final AuditLedgerService auditLedgerService;

    public FederatedOutboxDispatchService(OutboxPublisher outboxPublisher,
                                          AuditLedgerService auditLedgerService) {
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    public UUID dispatch(String topic,
                         String eventCode,
                         String sourceSystem,
                         String sourceTribunal,
                         String aggregateType,
                         String aggregateId,
                         String causalityKey,
                         Long aggregateVersion,
                         Map<String, Object> attributes,
                         Object payload) {
        Instant now = Instant.now();
        String normalizedAggregateType = safeText(aggregateType, 120, "FEDERATED_EVENT");
        String normalizedAggregateId = safeText(aggregateId, 180, "N/A");
        String envelopeId = UUID.randomUUID().toString();
        String payloadHash = Hashes.sha256Hex(String.valueOf(payload));
        String idempotencyKey = Hashes.sha256Hex(String.join("|",
                safeText(eventCode, 120, "EVENT"),
                safeText(sourceSystem, 60, "PJB"),
                safeText(sourceTribunal, 30, "NACIONAL"),
                normalizedAggregateType,
                normalizedAggregateId,
                safeText(causalityKey, 160, normalizedAggregateId),
                String.valueOf(aggregateVersion == null ? 0L : aggregateVersion),
                payloadHash));
        FederatedEventEnvelope envelope = new FederatedEventEnvelope(
                envelopeId,
                safeText(eventCode, 120, "EVENT"),
                safeText(sourceSystem, 60, "PJB"),
                safeText(sourceTribunal, 30, "NACIONAL"),
                normalizedAggregateType,
                normalizedAggregateId,
                safeText(causalityKey, 160, normalizedAggregateId),
                idempotencyKey,
                aggregateVersion,
                now,
                now,
                attributes == null ? Map.of() : Map.copyOf(attributes),
                payload
        );
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceSystem", envelope.sourceSystem());
        metadata.put("sourceTribunal", envelope.sourceTribunal());
        metadata.put("aggregateType", envelope.aggregateType());
        metadata.put("aggregateId", envelope.aggregateId());
        metadata.put("causalityKey", envelope.causalityKey());
        metadata.put("idempotencyKey", envelope.idempotencyKey());
        metadata.put("aggregateVersion", envelope.aggregateVersion());
        metadata.put("payloadHash", payloadHash);
        UUID outboxId = outboxPublisher.enqueueTracked(
                safeText(topic, 180, normalizedAggregateType),
                envelope.eventCode(),
                envelope.toMap(),
                metadata,
                envelope.idempotencyKey(),
                normalizedAggregateType,
                normalizedAggregateId
        );
        auditLedgerService.appendSafely(envelope.eventCode(), normalizedAggregateType, normalizedAggregateId, payloadHash, "FEDERATED_OUTBOX_DISPATCH:" + envelope.envelopeId());
        return outboxId;
    }

    private String safeText(String value, int max, String fallback) {
        String effective = value == null || value.isBlank() ? fallback : value.trim();
        return effective.length() <= max ? effective : effective.substring(0, max);
    }
}
