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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistAuditEventType;

@Entity
@Table(
    name = "tb_atendimento_checklist_audit",
    indexes = {
        @Index(name = "idx_atendimento_chk_audit_thread", columnList = "thread_id, id"),
        @Index(name = "idx_atendimento_chk_audit_item", columnList = "item_id, id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoChecklistAuditEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "actor_user_id", nullable = false)
  private Long actorUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 20)
  private AtendimentoChecklistAuditEventType eventType;

  @Column(name = "payload_json", columnDefinition = "TEXT")
  private String payloadJson;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "prev_hash", length = 64)
  private String prevHash;

  @Column(name = "chain_hash", nullable = false, length = 64)
  private String chainHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
