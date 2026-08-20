package com.tcc.pjb.backend.model.entity.pericia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_perito_sorteio_audit",
        indexes = {
                @Index(name = "idx_perito_sorteio_actor", columnList = "actor_id,created_at"),
                @Index(name = "idx_perito_sorteio_processo", columnList = "processo_id,created_at"),
                @Index(name = "idx_perito_sorteio_perito", columnList = "perito_id,created_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PeritoSorteioAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Usuario actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disponibilidade_id", nullable = false)
    private PeritoDisponibilidade disponibilidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perito_id", nullable = false)
    private Usuario perito;

    @Column(name = "especialidade_codigo", nullable = false, length = 120)
    private String especialidadeCodigo;

    @Column(name = "comarca", length = 120)
    private String comarca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    private Comarca comarcaEntidade;

    @Column(name = "data_referencia", nullable = false, length = 20)
    private String dataReferencia;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "nomeacoes_ativas", nullable = false)
    private Long nomeacoesAtivas;

    @Column(name = "candidatos_eligiveis", nullable = false)
    private Integer candidatosElegiveis;

    @Column(name = "fundamentos_json", columnDefinition = "TEXT")
    private String fundamentosJson;

    @Column(name = "hash_integridade", nullable = false, length = 64)
    private String hashIntegridade;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (score == null) {
            score = 0.0d;
        }
        if (nomeacoesAtivas == null) {
            nomeacoesAtivas = 0L;
        }
        if (candidatosElegiveis == null) {
            candidatosElegiveis = 0;
        }
    }
}
