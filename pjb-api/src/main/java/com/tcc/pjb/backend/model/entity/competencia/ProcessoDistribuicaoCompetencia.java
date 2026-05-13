package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_distribuicao_competencia", indexes = {
        @Index(name = "idx_proc_dist_comp_processo", columnList = "processo_id"),
        @Index(name = "idx_proc_dist_comp_unidade", columnList = "unidade_id"),
        @Index(name = "idx_proc_dist_comp_status", columnList = "status"),
        @Index(name = "idx_proc_dist_comp_nupn", columnList = "nupn"),
        @Index(name = "idx_proc_dist_comp_request_hash", columnList = "request_hash")
})
public class ProcessoDistribuicaoCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeJudiciariaCompetencia unidade;

    @Column(name = "nupn", length = 50)
    private String nupn;

    @Column(name = "score_final", nullable = false)
    private double scoreFinal;

    @Column(name = "score_territorial", nullable = false)
    private double scoreTerritorial;

    @Column(name = "score_especialidade", nullable = false)
    private double scoreEspecialidade;

    @Column(name = "score_disponibilidade", nullable = false)
    private double scoreDisponibilidade;

    @Column(name = "score_equilibrio", nullable = false)
    private double scoreEquilibrio;

    @Column(name = "score_aderencia_normativa", nullable = false)
    private double scoreAderenciaNormativa;

    @Column(name = "distribuicao_automatica", nullable = false)
    private boolean distribuicaoAutomatica;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusDistribuicaoCompetencia status;

    @Column(name = "motivacao", columnDefinition = "text")
    private String motivacao;

    @Column(name = "alertas", columnDefinition = "text")
    private String alertas;

    @Column(name = "fatores_revisao", columnDefinition = "text")
    private String fatoresRevisao;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public ProcessoDistribuicaoCompetencia(Processo processo,
                                           UnidadeJudiciariaCompetencia unidade,
                                           String nupn,
                                           double scoreFinal,
                                           double scoreTerritorial,
                                           double scoreEspecialidade,
                                           double scoreDisponibilidade,
                                           double scoreEquilibrio,
                                           double scoreAderenciaNormativa,
                                           boolean distribuicaoAutomatica,
                                           StatusDistribuicaoCompetencia status,
                                           String motivacao,
                                           String alertas,
                                           String fatoresRevisao,
                                           String requestHash) {
        this.processo = processo;
        this.unidade = unidade;
        this.nupn = nupn;
        this.scoreFinal = scoreFinal;
        this.scoreTerritorial = scoreTerritorial;
        this.scoreEspecialidade = scoreEspecialidade;
        this.scoreDisponibilidade = scoreDisponibilidade;
        this.scoreEquilibrio = scoreEquilibrio;
        this.scoreAderenciaNormativa = scoreAderenciaNormativa;
        this.distribuicaoAutomatica = distribuicaoAutomatica;
        this.status = status;
        this.motivacao = motivacao;
        this.alertas = alertas;
        this.fatoresRevisao = fatoresRevisao;
        this.requestHash = requestHash;
    }

    @PrePersist
    void prePersist() {
        Instant agora = Instant.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
