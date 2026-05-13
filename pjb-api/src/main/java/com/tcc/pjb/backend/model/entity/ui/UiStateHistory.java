package com.tcc.pjb.backend.model.entity.ui;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_ui_state_history")
public class UiStateHistory {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, length = 30)
  private UiSubjectType subjectType;

  @Column(name = "processo_id")
  private Long processoId;

  @Column(name = "work_item_id")
  private Long workItemId;

  @Column(name = "inbox_key", length = 180)
  private String inboxKey;

  @Column(name = "event_type", nullable = false, length = 120)
  private String eventType;

  @Column(name = "from_status", length = 80)
  private String fromStatus;

  @Column(name = "to_status", length = 80)
  private String toStatus;

  @Column(name = "from_tokens_json", columnDefinition = "TEXT")
  private String fromTokensJson;

  @Column(name = "to_tokens_json", columnDefinition = "TEXT")
  private String toTokensJson;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "actor_role", length = 60)
  private String actorRole;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected UiStateHistory() {
  }

  public UiStateHistory(
      UUID id,
      UiSubjectType subjectType,
      Long processoId,
      Long workItemId,
      String inboxKey,
      String eventType,
      String fromStatus,
      String toStatus,
      String fromTokensJson,
      String toTokensJson,
      Long actorUserId,
      String actorRole,
      String message,
      Instant occurredAt
  ) {
    this.id = Objects.requireNonNull(id, "id");
    this.subjectType = Objects.requireNonNull(subjectType, "subjectType");
    this.processoId = processoId;
    this.workItemId = workItemId;
    this.inboxKey = normalizeNullable(inboxKey);
    this.eventType = normalize(eventType);
    this.fromStatus = normalizeNullable(fromStatus);
    this.toStatus = normalizeNullable(toStatus);
    this.fromTokensJson = fromTokensJson;
    this.toTokensJson = toTokensJson;
    this.actorUserId = actorUserId;
    this.actorRole = normalizeNullable(actorRole);
    this.message = message;
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
  }

  public UUID getId() {
    return id;
  }

  public UiSubjectType getSubjectType() {
    return subjectType;
  }

  public Long getProcessoId() {
    return processoId;
  }

  public Long getWorkItemId() {
    return workItemId;
  }

  public String getInboxKey() {
    return inboxKey;
  }

  public String getEventType() {
    return eventType;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public String getFromTokensJson() {
    return fromTokensJson;
  }

  public String getToTokensJson() {
    return toTokensJson;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public String getActorRole() {
    return actorRole;
  }

  public String getMessage() {
    return message;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  private static String normalize(String v) {
    Objects.requireNonNull(v, "value");
    String s = v.trim();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("blank value");
    }
    return s;
  }

  private static String normalizeNullable(String v) {
    if (v == null) return null;
    String s = v.trim();
    return s.isEmpty() ? null : s;
  }
}
