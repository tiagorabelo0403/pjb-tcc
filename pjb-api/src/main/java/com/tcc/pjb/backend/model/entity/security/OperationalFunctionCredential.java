package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "operational_function_credentials",
        uniqueConstraints = @UniqueConstraint(name = "uk_ofc_usuario_function", columnNames = {"usuario_id", "function_code"}))
public class OperationalFunctionCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "function_code", nullable = false, length = 80)
    private String functionCode;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "secret_hash", length = 255)
    private String secretHash;

    @Column(name = "justica_axis", length = 32)
    private String justicaAxis;

    @Column(name = "tribunal_codigo", length = 32)
    private String tribunalCodigo;

    @Column(name = "forum_code", length = 96)
    private String forumCode;

    @Column(name = "unit_code", length = 128)
    private String unitCode;

    @Column(name = "vara_label", length = 160)
    private String varaLabel;

    @Column(name = "uf", length = 8)
    private String uf;

    @Column(name = "comarca", length = 160)
    private String comarca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by_user_id")
    private Usuario managedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provisioned_by_user_id")
    private Usuario provisionedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_rotation_by_user_id")
    private Usuario lastRotationByUser;

    @Column(name = "reason", length = 600)
    private String reason;

    @Column(name = "audit_trail_json", columnDefinition = "TEXT")
    private String auditTrailJson;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null || status.isBlank()) {
            status = "PENDING_SETUP";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getFunctionCode() { return functionCode; }
    public void setFunctionCode(String functionCode) { this.functionCode = functionCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }
    public String getJusticaAxis() { return justicaAxis; }
    public void setJusticaAxis(String justicaAxis) { this.justicaAxis = justicaAxis; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public void setTribunalCodigo(String tribunalCodigo) { this.tribunalCodigo = tribunalCodigo; }
    public String getForumCode() { return forumCode; }
    public void setForumCode(String forumCode) { this.forumCode = forumCode; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public String getVaraLabel() { return varaLabel; }
    public void setVaraLabel(String varaLabel) { this.varaLabel = varaLabel; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getComarca() { return comarca; }
    public void setComarca(String comarca) { this.comarca = comarca; }
    public Usuario getManagedByUser() { return managedByUser; }
    public void setManagedByUser(Usuario managedByUser) { this.managedByUser = managedByUser; }
    public Usuario getProvisionedByUser() { return provisionedByUser; }
    public void setProvisionedByUser(Usuario provisionedByUser) { this.provisionedByUser = provisionedByUser; }
    public Usuario getLastRotationByUser() { return lastRotationByUser; }
    public void setLastRotationByUser(Usuario lastRotationByUser) { this.lastRotationByUser = lastRotationByUser; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAuditTrailJson() { return auditTrailJson; }
    public void setAuditTrailJson(String auditTrailJson) { this.auditTrailJson = auditTrailJson; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public LocalDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public LocalDateTime getLastResetAt() { return lastResetAt; }
    public void setLastResetAt(LocalDateTime lastResetAt) { this.lastResetAt = lastResetAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}
