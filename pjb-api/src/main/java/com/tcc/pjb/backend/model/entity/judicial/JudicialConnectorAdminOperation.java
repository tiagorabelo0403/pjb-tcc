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
@Table(name = "tb_judicial_connector_admin_operation")
public class JudicialConnectorAdminOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_system", nullable = false, length = 40)
    private JudicialSystem connectorSystem;

    @Column(name = "tribunal_codigo", length = 20)
    private String tribunalCodigo;

    @Column(name = "environment_name", length = 60)
    private String environmentName;

    @Column(name = "operation_type", nullable = false, length = 80)
    private String operationType;

    @Column(name = "requested_by", length = 160)
    private String requestedBy;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "outcome_status", nullable = false, length = 80)
    private String outcomeStatus;

    @Column(name = "outcome_message", length = 1000)
    private String outcomeMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public JudicialSystem getConnectorSystem() { return connectorSystem; }
    public void setConnectorSystem(JudicialSystem connectorSystem) { this.connectorSystem = connectorSystem; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public void setTribunalCodigo(String tribunalCodigo) { this.tribunalCodigo = tribunalCodigo; }
    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }
    public String getOutcomeMessage() { return outcomeMessage; }
    public void setOutcomeMessage(String outcomeMessage) { this.outcomeMessage = outcomeMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
