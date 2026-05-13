package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_atendimento_message",
    indexes = {
        @Index(name = "idx_atendimento_message_thread_id", columnList = "thread_id, id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "sender_usuario_id", nullable = false)
  private Long senderUsuarioId;

  @Column(name = "sender_tipo", nullable = false, length = 40)
  private String senderTipo;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  



  @Column(name = "reply_to_message_id")
  private Long replyToMessageId;

  



  @Column(name = "client_msg_id")
  private UUID clientMessageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private AtendimentoMessageStatus status;

  @Column(name = "blocked_reason", length = 64)
  private String blockedReason;

  @Column(name = "blocked_note", length = 200)
  private String blockedNote;

  @Column(name = "blocked_at")
  private Instant blockedAt;

  @Column(name = "blocked_by_user_id")
  private Long blockedByUserId;

  @Column(name = "prev_hash", length = 64)
  private String prevHash;

  @Column(name = "msg_hash", nullable = false, length = 64)
  private String msgHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
