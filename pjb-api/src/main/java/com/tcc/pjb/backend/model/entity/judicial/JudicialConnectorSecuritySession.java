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
@Table(name = "tb_judicial_connector_security_session")
public class JudicialConnectorSecuritySession {

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

    @Column(name = "operation_name", nullable = false, length = 120)
    private String operationName;

    @Column(name = "target_scheme", length = 20)
    private String targetScheme;

    @Column(name = "target_host_sha256", length = 64)
    private String targetHostSha256;

    @Column(name = "target_port")
    private Integer targetPort;

    @Column(name = "tls_mode", length = 20)
    private String tlsMode;

    @Column(name = "outcome_status", nullable = false, length = 60)
    private String outcomeStatus;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "duration_millis", nullable = false)
    private long durationMillis;

    @Column(name = "hardware_backed", nullable = false)
    private boolean hardwareBacked;

    @Column(name = "mutual_tls", nullable = false)
    private boolean mutualTls;

    @Column(name = "hostname_verification", nullable = false)
    private boolean hostnameVerification;

    @Column(name = "key_store_ref", length = 160)
    private String keyStoreRef;

    @Column(name = "trust_store_ref", length = 160)
    private String trustStoreRef;

    @Column(name = "key_alias", length = 255)
    private String keyAlias;

    @Column(name = "correlation_id", length = 200)
    private String correlationId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

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

    public JudicialSystem getConnectorSystem() {
        return connectorSystem;
    }

    public void setConnectorSystem(JudicialSystem connectorSystem) {
        this.connectorSystem = connectorSystem;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public void setTribunalCodigo(String tribunalCodigo) {
        this.tribunalCodigo = tribunalCodigo;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getTargetScheme() {
        return targetScheme;
    }

    public void setTargetScheme(String targetScheme) {
        this.targetScheme = targetScheme;
    }

    public String getTargetHostSha256() {
        return targetHostSha256;
    }

    public void setTargetHostSha256(String targetHostSha256) {
        this.targetHostSha256 = targetHostSha256;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    public String getTlsMode() {
        return tlsMode;
    }

    public void setTlsMode(String tlsMode) {
        this.tlsMode = tlsMode;
    }

    public String getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(String outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public boolean isHardwareBacked() {
        return hardwareBacked;
    }

    public void setHardwareBacked(boolean hardwareBacked) {
        this.hardwareBacked = hardwareBacked;
    }

    public boolean isMutualTls() {
        return mutualTls;
    }

    public void setMutualTls(boolean mutualTls) {
        this.mutualTls = mutualTls;
    }

    public boolean isHostnameVerification() {
        return hostnameVerification;
    }

    public void setHostnameVerification(boolean hostnameVerification) {
        this.hostnameVerification = hostnameVerification;
    }

    public String getKeyStoreRef() {
        return keyStoreRef;
    }

    public void setKeyStoreRef(String keyStoreRef) {
        this.keyStoreRef = keyStoreRef;
    }

    public String getTrustStoreRef() {
        return trustStoreRef;
    }

    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
