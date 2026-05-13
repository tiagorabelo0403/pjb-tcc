package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "tb_atendimento_thread_policy",
    indexes = {
        @Index(name = "idx_atendimento_tp_updated_by", columnList = "updated_by_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoThreadPolicy {

  @Id
  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "cidadao_send_disabled_until")
  private Instant cidadaoSendDisabledUntil;

  @Column(name = "updated_by_user_id")
  private Long updatedByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
