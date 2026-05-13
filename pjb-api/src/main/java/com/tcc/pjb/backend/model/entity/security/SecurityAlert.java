package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import com.tcc.pjb.backend.model.entity.Usuario;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "security_alerts",
        indexes = {
                @Index(name = "idx_alert_user", columnList = "usuario_id, criado_em"),
                @Index(name = "idx_alert_type", columnList = "tipo, criado_em")
        }
)
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_alert_usuario"))
    private Usuario usuario;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String tipo;

    @NotBlank
    @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String titulo;

    @Size(max = 2000)
    @Column(length = 2000)
    private String detalhes;

    @Size(max = 64)
    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "ack", nullable = false)
    private boolean acknowledged;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAlert)) return false;
        SecurityAlert that = (SecurityAlert) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
