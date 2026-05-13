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
        name = "webauthn_sessions",
        indexes = {
                @Index(name = "idx_webauthn_session_user", columnList = "usuario_id, expires_at"),
                @Index(name = "idx_webauthn_session_expires", columnList = "expires_at")
        }
)
public class WebAuthnSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_webauthn_session_usuario"))
    private com.tcc.pjb.backend.model.entity.Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 24)
    private String tipo;

    @Column(name = "request_json", nullable = false, length = 12000)
    private String requestJson;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public com.tcc.pjb.backend.model.entity.Usuario getUsuario() { return usuario; }
    public void setUsuario(com.tcc.pjb.backend.model.entity.Usuario usuario) { this.usuario = usuario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String requestJson) { this.requestJson = requestJson; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebAuthnSession)) return false;
        WebAuthnSession that = (WebAuthnSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
