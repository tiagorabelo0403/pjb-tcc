package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_judicial_connector_telemetry")
public class JudicialConnectorTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "numero_unificado", length = 50)
    private String numeroUnificado;

    @Column(name = "tribunal_codigo", length = 20)
    private String tribunalCodigo;

    @Column(name = "unidade_judiciaria_codigo", length = 80)
    private String unidadeJudiciariaCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_system", length = 40)
    private JudicialSystem connectorSystem;

    @Column(name = "event_type", length = 60, nullable = false)
    private String eventType;

    @Column(name = "status", length = 80)
    private String status;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "protocol_reference", length = 120)
    private String protocolReference;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public String getNumeroUnificado() {
        return numeroUnificado;
    }

    public void setNumeroUnificado(String numeroUnificado) {
        this.numeroUnificado = numeroUnificado;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public void setTribunalCodigo(String tribunalCodigo) {
        this.tribunalCodigo = tribunalCodigo;
    }

    public String getUnidadeJudiciariaCodigo() {
        return unidadeJudiciariaCodigo;
    }

    public void setUnidadeJudiciariaCodigo(String unidadeJudiciariaCodigo) {
        this.unidadeJudiciariaCodigo = unidadeJudiciariaCodigo;
    }

    public JudicialSystem getConnectorSystem() {
        return connectorSystem;
    }

    public void setConnectorSystem(JudicialSystem connectorSystem) {
        this.connectorSystem = connectorSystem;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public String getProtocolReference() {
        return protocolReference;
    }

    public void setProtocolReference(String protocolReference) {
        this.protocolReference = protocolReference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
