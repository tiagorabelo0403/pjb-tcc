package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemKind;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;

@Entity
@Table(
    name = "tb_atendimento_checklist_item",
    indexes = {
        @Index(name = "idx_atendimento_chk_thread", columnList = "thread_id, id"),
        @Index(name = "idx_atendimento_chk_thread_status", columnList = "thread_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoChecklistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 16)
  private AtendimentoChecklistItemKind kind;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private AtendimentoChecklistItemStatus status;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "note", length = 800)
  private String note;

  @Column(name = "due_at")
  private Instant dueAt;

  
  @Column(name = "documento_id")
  private Long documentoId;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "completed_by_user_id")
  private Long completedByUserId;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancelled_by_user_id")
  private Long cancelledByUserId;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;


@PrePersist
@PreUpdate
private void validateInvariants() {
  
  AtendimentoChecklistItemStatus st = this.status;
  if (st == null) {
    throw new IllegalStateException("checklist_item_status_required");
  }

  
  if (createdAt != null && updatedAt != null && updatedAt.isBefore(createdAt)) {
    throw new IllegalStateException("checklist_item_updated_before_created");
  }

  switch (st) {
    case OPEN -> {
      if (completedAt != null || completedByUserId != null || cancelledAt != null || cancelledByUserId != null) {
        throw new IllegalStateException("checklist_item_open_must_not_have_done_or_cancel_fields");
      }
    }
    case DONE -> {
      if (completedAt == null || completedByUserId == null) {
        throw new IllegalStateException("checklist_item_done_requires_completed_fields");
      }
      if (cancelledAt != null || cancelledByUserId != null) {
        throw new IllegalStateException("checklist_item_done_must_not_have_cancel_fields");
      }
    }
    case CANCELLED -> {
      if (cancelledAt == null || cancelledByUserId == null) {
        throw new IllegalStateException("checklist_item_cancelled_requires_cancel_fields");
      }
      if (completedAt != null || completedByUserId != null) {
        throw new IllegalStateException("checklist_item_cancelled_must_not_have_done_fields");
      }
    }
  }
}
}
