package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_break_glass_access_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakGlassAccessSession {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "nupn", length = 50)
    private String nupn;

    @Column(name = "requested_by_usuario_id", nullable = false)
    private Long requestedByUsuarioId;

    @Column(name = "requested_by_profile", nullable = false, length = 60)
    private String requestedByProfile;

    @Column(name = "access_scope", nullable = false, length = 60)
    private String accessScope;

    @Column(name = "justification", nullable = false, length = 1000)
    private String justification;

    @Column(name = "approval_basis", length = 240)
    private String approvalBasis;

    @Column(name = "risk_level", nullable = false, length = 40)
    private String riskLevel;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "step_up_required", nullable = false)
    private boolean stepUpRequired;

    @Column(name = "step_up_satisfied", nullable = false)
    private boolean stepUpSatisfied;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(name = "audit_hash", nullable = false, length = 64)
    private String auditHash;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
