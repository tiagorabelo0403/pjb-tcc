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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_atendimento_reminder",
    indexes = {
        @Index(name = "idx_atendimento_reminder_thread", columnList = "thread_id, fire_at"),
        @Index(name = "idx_atendimento_reminder_due", columnList = "status, fire_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoReminder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Column(name = "target_user_id")
  private Long targetUserId;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "fire_at", nullable = false)
  private Instant fireAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private AtendimentoReminderStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", length = 180)
  private String lastError;

  @Column(name = "sent_message_id")
  private Long sentMessageId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
