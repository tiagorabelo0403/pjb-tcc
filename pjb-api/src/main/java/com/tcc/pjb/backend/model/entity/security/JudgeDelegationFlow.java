package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
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
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationScope;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_judge_delegation_flow", indexes = {
        @Index(name = "idx_jdf_mag_status", columnList = "magistrate_user_id,status,expires_at"),
        @Index(name = "idx_jdf_delegate_status", columnList = "delegate_user_id,status,expires_at"),
        @Index(name = "idx_jdf_jti", columnList = "token_jti"),
        @Index(name = "idx_jdf_uuid", columnList = "request_uuid", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeDelegationFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID requestUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "magistrate_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jdf_magistrate"))
    private Usuario magistrate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jdf_delegate"))
    private Usuario delegate;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 40)
    private DelegationScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JudgeDelegationFlowStatus status;

    @Column(name = "requested_reason", length = 600)
    private String requestedReason;

    @Column(name = "device_binding_hash", length = 128)
    private String deviceBindingHash;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "token_jti", length = 80)
    private String tokenJti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", foreignKey = @ForeignKey(name = "fk_jdf_approved_by"))
    private Usuario approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id", foreignKey = @ForeignKey(name = "fk_jdf_rejected_by"))
    private Usuario rejectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id", foreignKey = @ForeignKey(name = "fk_jdf_revoked_by"))
    private Usuario revokedBy;

    public boolean isActiveAt(LocalDateTime at) {
        return status == JudgeDelegationFlowStatus.APROVADA
                && expiresAt != null
                && at != null
                && !expiresAt.isBefore(at);
    }
}
