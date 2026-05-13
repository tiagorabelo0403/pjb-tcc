package com.tcc.pjb.backend.model.entity.secretariat;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_secretariat_queue_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretariatQueueItem {

  @Id
  @Column(name = "work_item_id", nullable = false)
  private Long workItemId;

  @Column(name = "processo_id", nullable = false)
  private Long processoId;

  @Column(name = "inbox_key", nullable = false, length = 120)
  private String inboxKey;

  @Column(name = "queue_code", length = 120)
  private String queueCode;

  @Column(name = "lane_code", length = 40)
  private String laneCode;

  @Column(name = "desk_axis", length = 80)
  private String deskAxis;

  @Column(name = "workload_band", length = 40)
  private String workloadBand;

  @Column(name = "routing_fingerprint", length = 240)
  private String routingFingerprint;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "prioridade", nullable = false)
  private Integer prioridade;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "score", nullable = false)
  private Integer score;

  @Column(name = "tags_json", columnDefinition = "TEXT")
  private String tagsJson;

  @Column(name = "metadata_json", columnDefinition = "TEXT")
  private String metadataJson;

  @Column(name = "titulo", nullable = false, length = 220)
  private String titulo;

  @Column(name = "escalation_required", nullable = false)
  private boolean escalationRequired;

  @Column(name = "secrecy_review_required", nullable = false)
  private boolean secrecyReviewRequired;

  @Column(name = "hearing_sensitive", nullable = false)
  private boolean hearingSensitive;

  @Column(name = "blocking", nullable = false)
  private boolean blocking;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

}
