package com.tcc.pjb.backend.model.entity.workflow;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_movimentacao_adjustment_audit", indexes = {
        @Index(name = "idx_maa_processo_created", columnList = "processo_id,created_at"),
        @Index(name = "idx_maa_mov_status", columnList = "movimentacao_id,status"),
        @Index(name = "idx_maa_uuid", columnList = "request_uuid", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoAdjustmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID requestUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_maa_processo"))
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_maa_movimentacao"))
    private MovimentacaoProcessual movimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_maa_requested_by"))
    private Usuario requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    private MovimentacaoAdjustmentMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private MovimentacaoAdjustmentStatus status;

    @Column(name = "motivo", nullable = false, length = 800)
    private String motivo;

    @Column(name = "descricao_substitutiva", columnDefinition = "TEXT")
    private String descricaoSubstitutiva;

    @Column(name = "original_hash", nullable = false, length = 64)
    private String originalHash;

    @Column(name = "audit_hash", nullable = false, length = 64)
    private String auditHash;

    @Column(name = "compliance_score", nullable = false)
    private Integer complianceScore;

    @Column(name = "compliance_flags", length = 1200)
    private String complianceFlags;

    @Column(name = "compliance_verdict", nullable = false, length = 32)
    private String complianceVerdict;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "ledger_entry_hash", length = 64)
    private String ledgerEntryHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_movimentacao_id", foreignKey = @ForeignKey(name = "fk_maa_generated_mov"))
    private MovimentacaoProcessual generatedMovimentacao;
}
