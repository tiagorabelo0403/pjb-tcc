package com.tcc.pjb.backend.model.entity.julgamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_julgamento_colegiado",
    indexes = {
        @Index(name = "idx_julgamento_processo", columnList = "processo_id"),
        @Index(name = "idx_julgamento_grau_status", columnList = "grau,status")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JulgamentoColegiado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processo_id", nullable = false)
  private Processo processo;

  @Enumerated(EnumType.STRING)
  @Column(name = "grau", nullable = false, length = 30)
  private GrauJurisdicao grau;

  @Column(name = "tribunal_sigla", length = 40)
  private String tribunalSigla;

  @Column(name = "orgao_julgador", length = 120)
  private String orgaoJulgador;

  @Column(name = "relator_nome", length = 120)
  private String relatorNome;

  @Column(name = "revisor_nome", length = 120)
  private String revisorNome;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private StatusJulgamentoColegiado status;

  @Column(name = "pauta_data_hora")
  private LocalDateTime pautaDataHora;

  @Column(name = "sessao_inicio")
  private LocalDateTime sessaoInicio;

  @Column(name = "sessao_fim")
  private LocalDateTime sessaoFim;

  @Column(name = "placar_favor")
  private Integer placarFavor;

  @Column(name = "placar_contra")
  private Integer placarContra;

  @Column(name = "placar_parcial")
  private Integer placarParcial;

  @Column(name = "placar_outros")
  private Integer placarOutros;

  @Column(name = "acordao_publicado")
  private Boolean acordaoPublicado;

  @Column(name = "acordao_publicado_em")
  private LocalDateTime acordaoPublicadoEm;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (status == null) status = StatusJulgamentoColegiado.AGENDADO;
    if (placarFavor == null) placarFavor = 0;
    if (placarContra == null) placarContra = 0;
    if (placarParcial == null) placarParcial = 0;
    if (placarOutros == null) placarOutros = 0;
    if (acordaoPublicado == null) acordaoPublicado = Boolean.FALSE;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
