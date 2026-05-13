package com.tcc.pjb.backend.model.entity.defensor;

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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_defensoria_vulnerabilidade_caso",
        indexes = {
                @Index(name = "idx_def_vuln_defensor", columnList = "defensor_id,created_at"),
                @Index(name = "idx_def_vuln_status", columnList = "status,prioridade_faixa"),
                @Index(name = "idx_def_vuln_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class DefensoriaVulnerabilidadeCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defensor_id", nullable = false)
    private Usuario defensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "assistido_nome", nullable = false, length = 180)
    private String assistidoNome;

    @Column(name = "documento_identificador", length = 40)
    private String documentoIdentificador;

    @Column(name = "score_vulnerabilidade", nullable = false)
    private Integer scoreVulnerabilidade;

    @Column(name = "prioridade_faixa", nullable = false, length = 30)
    private String prioridadeFaixa;

    @Column(name = "hipervulnerabilidades_json", columnDefinition = "TEXT")
    private String hipervulnerabilidadesJson;

    @Column(name = "sinais_risco_json", columnDefinition = "TEXT")
    private String sinaisRiscoJson;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "PRIORIZADO";
        }
        if (prioridadeFaixa == null || prioridadeFaixa.isBlank()) {
            prioridadeFaixa = "ALTA";
        }
        if (scoreVulnerabilidade == null) {
            scoreVulnerabilidade = 0;
        }
    }
}
