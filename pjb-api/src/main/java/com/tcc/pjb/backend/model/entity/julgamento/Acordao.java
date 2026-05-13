package com.tcc.pjb.backend.model.entity.julgamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_acordao",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_acordao_julgamento", columnNames = {"julgamento_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acordao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "julgamento_id", nullable = false)
  private JulgamentoColegiado julgamento;

  @Column(name = "numero_acordao", length = 80)
  private String numeroAcordao;

  @Lob
  @Column(name = "ementa_resumo", columnDefinition = "TEXT")
  private String ementaResumo;

  @Column(name = "inteiro_teor_ref", length = 300)
  private String inteiroTeorRef;

  @Column(name = "publicado_em")
  private LocalDateTime publicadoEm;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
