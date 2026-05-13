package com.tcc.pjb.backend.model.entity.secretariat;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_documento_triage_signal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoTriageSignal {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "doc_id", nullable = false)
  private UUID docId;

  @Column(name = "processo_id", nullable = false)
  private Long processoId;

  @Column(name = "work_item_id")
  private Long workItemId;

  @Column(name = "urgency_level", nullable = false, length = 20)
  private String urgencyLevel;

  @Column(name = "score", nullable = false)
  private Integer score;

  @Column(name = "tags_json", nullable = false, length = 12000)
  private String tagsJson;

  @Column(name = "keywords", nullable = false, columnDefinition = "text")
  private String keywords;

  @Column(name = "rationale", nullable = false, columnDefinition = "text")
  private String rationale;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
