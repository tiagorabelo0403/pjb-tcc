package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.Objects;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import com.tcc.pjb.backend.model.entity.Usuario;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "dual_approval_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dar_request_key", columnNames = {"request_key"})
        },
        indexes = {
                @Index(name = "idx_dar_status", columnList = "status, expires_at"),
                @Index(name = "idx_dar_user", columnList = "requester_user_id, created_at")
        }
)
public class SecurityDualApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dar_requester"))
    private Usuario requester;

    @Column(name = "requester_device_id")
    private Long requesterDeviceId;

    @Column(name = "equipe_id")
    private Long equipeId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "method", nullable = false, length = 12)
    private String method;

    @Column(name = "path", nullable = false, length = 300)
    private String path;

    @Column(name = "rule_id", length = 64)
    private String ruleId;

    @Column(name = "action_hash", nullable = false, length = 64)
    private String actionHash;

    @Column(name = "request_key", nullable = false, length = 96)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SecurityDualApprovalStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", foreignKey = @ForeignKey(name = "fk_dar_approver"))
    private Usuario approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id", foreignKey = @ForeignKey(name = "fk_dar_rejector"))
    private Usuario rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getRequester() { return requester; }
    public void setRequester(Usuario requester) { this.requester = requester; }

    public Long getRequesterDeviceId() { return requesterDeviceId; }
    public void setRequesterDeviceId(Long requesterDeviceId) { this.requesterDeviceId = requesterDeviceId; }

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getActionHash() { return actionHash; }
    public void setActionHash(String actionHash) { this.actionHash = actionHash; }

    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }

    public SecurityDualApprovalStatus getStatus() { return status; }
    public void setStatus(SecurityDualApprovalStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Usuario getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Usuario approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Usuario getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(Usuario rejectedBy) { this.rejectedBy = rejectedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityDualApprovalRequest)) return false;
        SecurityDualApprovalRequest that = (SecurityDualApprovalRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
