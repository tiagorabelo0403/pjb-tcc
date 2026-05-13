package com.tcc.pjb.backend.modules.atendimento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_atendimento_moderation_event",
    indexes = {
        @Index(name = "idx_att_mod_event_at", columnList = "created_at"),
        @Index(name = "idx_att_mod_event_actor", columnList = "actor_user_id, created_at"),
        @Index(name = "idx_att_mod_event_thread", columnList = "thread_id, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoModerationEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "actor_user_id", nullable = false)
  private Long actorUserId;

  @Column(name = "actor_tipo", nullable = false, length = 40)
  private String actorTipo;

  @Column(name = "thread_id")
  private Long threadId;

  @Column(name = "processo_id")
  private Long processoId;

  @Column(name = "reason", nullable = false, length = 64)
  private String reason;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Column(name = "snippet", length = 200)
  private String snippet;

  @Column(name = "metadata_json", length = 12000)
  private String metadataJson;
}
