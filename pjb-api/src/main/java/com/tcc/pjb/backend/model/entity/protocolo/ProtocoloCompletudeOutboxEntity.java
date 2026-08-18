package com.tcc.pjb.backend.model.entity.protocolo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeEventoTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@PjbDataOwnership(module = PjbModuleId.AJUIZAMENTO, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "tb_protocolo_completude_outbox")
public class ProtocoloCompletudeOutboxEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "protocolo_id", nullable = false)
    private Long protocoloId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 60)
    private ProtocoloCompletudeEventoTipo tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "processado", nullable = false)
    private boolean processado;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "processado_em")
    private Instant processadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getProtocoloId() { return protocoloId; }
    public void setProtocoloId(Long protocoloId) { this.protocoloId = protocoloId; }
    public ProtocoloCompletudeEventoTipo getTipo() { return tipo; }
    public void setTipo(ProtocoloCompletudeEventoTipo tipo) { this.tipo = tipo; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public boolean isProcessado() { return processado; }
    public void setProcessado(boolean processado) { this.processado = processado; }
    public int getTentativas() { return tentativas; }
    public void setTentativas(int tentativas) { this.tentativas = tentativas; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getProcessadoEm() { return processadoEm; }
    public void setProcessadoEm(Instant processadoEm) { this.processadoEm = processadoEm; }
}
