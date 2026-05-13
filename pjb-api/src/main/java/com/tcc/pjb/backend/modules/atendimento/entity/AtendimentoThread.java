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
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoThreadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_atendimento_thread",
    indexes = {
        @Index(name = "idx_atendimento_thread_adv_updated", columnList = "advogado_id, updated_at"),
        @Index(name = "idx_atendimento_thread_cid_updated", columnList = "cidadao_usuario_id, updated_at"),
        @Index(name = "idx_atendimento_thread_processo", columnList = "processo_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoThread {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "processo_id", nullable = false)
  private Long processoId;

  @Column(name = "advogado_id", nullable = false)
  private Long advogadoId;

  @Column(name = "cidadao_usuario_id", nullable = false)
  private Long cidadaoUsuarioId;

  @Column(name = "cidadao_cpf_hash", nullable = false, length = 64)
  private String cidadaoCpfHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AtendimentoThreadStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_message_id")
  private Long lastMessageId;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
