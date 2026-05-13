package com.tcc.pjb.backend.model.entity.julgamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import com.tcc.pjb.backend.model.entity.julgamento.enums.PapelMagistradoNoJulgamento;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_voto_colegiado",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_voto_julgamento_ordem", columnNames = {"julgamento_id", "ordem"})
    },
    indexes = {
        @Index(name = "idx_voto_julgamento", columnList = "julgamento_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotoColegiado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "julgamento_id", nullable = false)
  private JulgamentoColegiado julgamento;

  @Column(name = "ordem", nullable = false)
  private Integer ordem;

  @Column(name = "magistrado_nome", nullable = false, length = 140)
  private String magistradoNome;

  @Column(name = "magistrado_cargo", length = 60)
  private String magistradoCargo;

  @Enumerated(EnumType.STRING)
  @Column(name = "papel", length = 30)
  private PapelMagistradoNoJulgamento papel;

  @Enumerated(EnumType.STRING)
  @Column(name = "voto_tipo", nullable = false, length = 60)
  private TipoVotoColegiado votoTipo;

  @Column(name = "voto_resumo", length = 800)
  private String votoResumo;

  @Column(name = "proferido_em", nullable = false)
  private LocalDateTime proferidoEm;

  @Column(name = "documento_ref", length = 300)
  private String documentoRef;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (proferidoEm == null) proferidoEm = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
