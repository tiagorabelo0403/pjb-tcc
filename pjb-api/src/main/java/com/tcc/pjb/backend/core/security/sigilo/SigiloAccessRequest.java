package com.tcc.pjb.backend.core.security.sigilo;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_sigilo_access_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SigiloAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "advogado_id", nullable = false)
    private Long advogadoId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SigiloAccessStatus status;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "hide_approver", nullable = false)
    private boolean hideApprover = true;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SigiloAccessStatus.PENDENTE;
        }
    }

    public boolean isApprovedAndActive(LocalDateTime now) {
        if (status != SigiloAccessStatus.APROVADA) return false;
        if (expiresAt == null) return false;
        return now.isBefore(expiresAt);
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }
}
