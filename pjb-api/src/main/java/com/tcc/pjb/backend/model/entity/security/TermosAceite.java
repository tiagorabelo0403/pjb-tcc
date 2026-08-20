package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.Usuario;
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
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_termos_aceite",
        indexes = {
                @Index(name = "idx_termos_aceite_usuario", columnList = "usuario_id, aceito_em")
        }
)
public class TermosAceite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_termos_aceite_usuario"))
    private Usuario usuario;

    @Column(name = "versao", nullable = false, length = 40)
    private String versao;

    @CreationTimestamp
    @Column(name = "aceito_em", nullable = false, updatable = false)
    private Instant aceitoEm;

    @Column(name = "ip", length = 64)
    private String ip;

    protected TermosAceite() {
    }

    public TermosAceite(Usuario usuario, String versao, String ip) {
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.versao = Objects.requireNonNull(versao, "versao");
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getVersao() {
        return versao;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }

    public String getIp() {
        return ip;
    }
}
