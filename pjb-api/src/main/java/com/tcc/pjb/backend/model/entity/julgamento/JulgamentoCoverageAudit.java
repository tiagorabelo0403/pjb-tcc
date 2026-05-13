package com.tcc.pjb.backend.model.entity.julgamento;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_julgamento_coverage_audit",
        indexes = {
                @Index(name = "idx_julg_cov_proc_created", columnList = "processo_id,created_at"),
                @Index(name = "idx_julg_cov_user_created", columnList = "usuario_id,created_at"),
                @Index(name = "idx_julg_cov_status_act", columnList = "overall_status,act_type")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class JulgamentoCoverageAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "act_type", nullable = false, length = 40)
    private String actType;

    @Column(name = "overall_status", nullable = false, length = 20)
    private String overallStatus;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "ramo_snapshot", length = 60)
    private String ramoSnapshot;

    @Column(name = "rito_snapshot", length = 80)
    private String ritoSnapshot;

    @Column(name = "justica_snapshot", length = 40)
    private String justicaSnapshot;

    @Column(name = "classe_snapshot", length = 180)
    private String classeSnapshot;

    @Column(name = "recursal_species", length = 20)
    private String recursalSpecies;

    @Column(name = "highlights_json", columnDefinition = "TEXT")
    private String highlightsJson;

    @Column(name = "alertas_json", columnDefinition = "TEXT")
    private String alertasJson;

    @Column(name = "bloqueios_json", columnDefinition = "TEXT")
    private String bloqueiosJson;

    @Column(name = "metadata_hash", length = 64)
    private String metadataHash;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (overallStatus == null || overallStatus.isBlank()) {
            overallStatus = "OK";
        }
        if (overallScore == null) {
            overallScore = 100;
        }
    }
}
