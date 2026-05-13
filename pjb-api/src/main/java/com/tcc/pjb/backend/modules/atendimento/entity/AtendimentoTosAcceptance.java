package com.tcc.pjb.backend.modules.atendimento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tb_atendimento_tos_acceptance")
public class AtendimentoTosAcceptance {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
