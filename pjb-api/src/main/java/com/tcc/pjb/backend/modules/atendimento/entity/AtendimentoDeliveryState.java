package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;







@Entity
@Table(name = "tb_atendimento_delivery_state")
@IdClass(AtendimentoDeliveryStateId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoDeliveryState {

  @Id
  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Id
  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "last_delivered_message_id")
  private Long lastDeliveredMessageId;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
