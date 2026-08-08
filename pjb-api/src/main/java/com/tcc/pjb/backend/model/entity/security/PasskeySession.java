package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "passkey_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_passkey_token", columnNames = {"token_hash"})
        },
        indexes = {
                @Index(name = "idx_passkey_user", columnList = "usuario_id, expires_at"),
                @Index(name = "idx_passkey_expires", columnList = "expires_at")
        }
)
public class PasskeySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_passkey_usuario"))
    private com.tcc.pjb.backend.model.entity.Usuario usuario;

    @Column(name = "device_id")
    private Long deviceId;


    @Column(name = "scope_action", length = 40)
    private String scopeAction;

    @Column(name = "scope_request_hash", length = 64)
    private String scopeRequestHash;

    @Column(name = "scope_one_shot", nullable = false)
    private boolean scopeOneShot = false;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    public boolean isRevogado() { return revogadoEm != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public com.tcc.pjb.backend.model.entity.Usuario getUsuario() { return usuario; }
    public void setUsuario(com.tcc.pjb.backend.model.entity.Usuario usuario) { this.usuario = usuario; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }


    public String getScopeAction() { return scopeAction; }
    public void setScopeAction(String scopeAction) { this.scopeAction = scopeAction; }

    public String getScopeRequestHash() { return scopeRequestHash; }
    public void setScopeRequestHash(String scopeRequestHash) { this.scopeRequestHash = scopeRequestHash; }

    public boolean isScopeOneShot() { return scopeOneShot; }
    public void setScopeOneShot(boolean scopeOneShot) { this.scopeOneShot = scopeOneShot; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public LocalDateTime getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(LocalDateTime revogadoEm) { this.revogadoEm = revogadoEm; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasskeySession)) return false;
        PasskeySession that = (PasskeySession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
