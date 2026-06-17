package com.tcc.pjb.backend.model.entity.outbox;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@IdClass(OutboxEventId.class)
@Table(name = "tb_outbox_event")
public class OutboxEvent {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "routing_key", nullable = false, length = 180)
  private String routingKey;

  @Column(name = "event_type", nullable = false, length = 120)
  private String eventType;

  @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
  private String payloadJson;

  @Column(name = "dedup_key", length = 200)
  private String dedupKey;

  @Column(name = "aggregate_type", length = 120)
  private String aggregateType;

  @Column(name = "aggregate_id", length = 180)
  private String aggregateId;

  @Column(name = "headers_json", columnDefinition = "TEXT")
  private String headersJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OutboxStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt;

  @Column(name = "locked_by", length = 120)
  private String lockedBy;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  @Id
  @Column(name = "created_month", nullable = false, updatable = false)
  private int createdMonth;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  private void initFields() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    ZonedDateTime zdt = createdAt.atZone(ZoneOffset.UTC);
    createdMonth = zdt.getYear() * 100 + zdt.getMonthValue();
  }

  protected OutboxEvent() {
  }

  public OutboxEvent(UUID id, String routingKey, String eventType, String payloadJson, Instant availableAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.routingKey = normalize(routingKey);
    this.eventType = normalize(eventType);
    this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
    this.dedupKey = null;
    this.aggregateType = null;
    this.aggregateId = null;
    this.headersJson = null;
    this.status = OutboxStatus.PENDING;
    this.attempts = 0;
    this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
  }

  public OutboxEvent(UUID id,
      String routingKey,
      String eventType,
      String payloadJson,
      Instant availableAt,
      String dedupKey,
      String aggregateType,
      String aggregateId,
      String headersJson) {
    this.id = Objects.requireNonNull(id, "id");
    this.routingKey = normalize(routingKey);
    this.eventType = normalize(eventType);
    this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
    this.dedupKey = dedupKey != null && !dedupKey.isBlank() ? dedupKey.trim() : null;
    this.aggregateType = aggregateType != null && !aggregateType.isBlank() ? aggregateType.trim() : null;
    this.aggregateId = aggregateId != null && !aggregateId.isBlank() ? aggregateId.trim() : null;
    this.headersJson = headersJson;
    this.status = OutboxStatus.PENDING;
    this.attempts = 0;
    this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
  }

  public UUID getId() {
    return id;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getHeadersJson() {
    return headersJson;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getAvailableAt() {
    return availableAt;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public int getCreatedMonth() {
    return createdMonth;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markInflight(String lockedBy, Instant lockedAt) {
    this.status = OutboxStatus.INFLIGHT;
    this.lockedBy = normalize(lockedBy);
    this.lockedAt = Objects.requireNonNull(lockedAt, "lockedAt");
  }

  public void markDone() {
    this.status = OutboxStatus.DONE;
    this.lockedBy = null;
    this.lockedAt = null;
    this.lastError = null;
  }

  public void markRetry(Instant nextAvailableAt, String err) {
    this.status = OutboxStatus.PENDING;
    this.attempts = this.attempts + 1;
    this.availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt");
    this.lastError = err;
    this.lockedBy = null;
    this.lockedAt = null;
  }

  public void markFailed(String err) {
    this.status = OutboxStatus.FAILED;
    this.lastError = err;
    this.lockedBy = null;
    this.lockedAt = null;
  }

  private static String normalize(String v) {
    Objects.requireNonNull(v, "value");
    String s = v.trim();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("blank value");
    }
    return s;
  }
}
