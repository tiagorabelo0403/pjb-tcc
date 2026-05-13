package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "strong_auth_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_strong_auth_once_req", columnNames = {"session_id", "request_hash"})
        },
        indexes = {
                @Index(name = "idx_sau_user", columnList = "usuario_id, criado_em"),
                @Index(name = "idx_sau_session", columnList = "session_id")
        }
)
public class StrongAuthUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sau_usuario"))
    private Usuario usuario;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "action_hash", nullable = false, length = 64)
    private String actionHash;


    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getActionHash() { return actionHash; }
    public void setActionHash(String actionHash) { this.actionHash = actionHash; }


    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrongAuthUsage)) return false;
        StrongAuthUsage that = (StrongAuthUsage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
