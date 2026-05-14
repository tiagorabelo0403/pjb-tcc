package com.tcc.pjb.backend.model.entity.processo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.service.processual.note.ProcessoNoteType;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_note")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "processo_id", nullable = false)
  private Long processoId;

  @Column(name = "author_usuario_id", nullable = false)
  private Long authorUsuarioId;

  @Column(name = "author_tipo", nullable = false, length = 24)
  private String authorTipo;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "tags_json", nullable = false, length = 12000)
  private String tagsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "note_type", length = 32)
  private ProcessoNoteType noteType;

  @Column(name = "visible_to_role", length = 64)
  private String visibleToRole;

  @Column(name = "visible_to_location", length = 128)
  private String visibleToLocation;

  @Column(name = "visible_until")
  private Instant visibleUntil;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "priority")
  private Integer priority;

  @Column(name = "sigilo_nivel", length = 24)
  private String sigiloNivel;
}
