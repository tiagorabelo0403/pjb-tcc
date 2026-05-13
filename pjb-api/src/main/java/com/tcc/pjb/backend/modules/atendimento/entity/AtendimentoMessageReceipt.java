package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;







@Entity
@Table(
    name = "tb_atendimento_message_receipt",
    indexes = {
        @Index(name = "idx_atendimento_receipt_thread_user_msg", columnList = "thread_id, usuario_id, message_id")
    }
)
@IdClass(AtendimentoMessageReceiptId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoMessageReceipt {

  @Id
  @Column(name = "message_id", nullable = false)
  private Long messageId;

  @Id
  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
