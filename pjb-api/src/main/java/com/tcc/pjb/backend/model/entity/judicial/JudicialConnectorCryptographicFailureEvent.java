package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptographicFailureType;
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
@Table(name = "tb_judicial_connector_crypto_failure")
public class JudicialConnectorCryptographicFailureEvent {

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

    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    @Column(name = "target_scheme", length = 20)
    private String targetScheme;

    @Column(name = "target_host_sha256", length = 64)
    private String targetHostSha256;

    @Column(name = "target_port")
    private Integer targetPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", nullable = false, length = 80)
    private JudicialConnectorCryptographicFailureType failureType;

    @Column(name = "failure_code", nullable = false, length = 100)
    private String failureCode;

    @Column(name = "failure_fingerprint", nullable = false, length = 64)
    private String failureFingerprint;

    @Column(name = "sanitized_message", nullable = false, length = 1000)
    private String sanitizedMessage;

    @Column(name = "key_alias", length = 255)
    private String keyAlias;

    @Column(name = "keystore_ref", length = 160)
    private String keyStoreRef;

    @Column(name = "truststore_ref", length = 160)
    private String trustStoreRef;

    @Column(name = "tls_mode", length = 20)
    private String tlsMode;

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

    public JudicialConnectorCryptographicFailureType getFailureType() {
        return failureType;
    }

    public void setFailureType(JudicialConnectorCryptographicFailureType failureType) {
        this.failureType = failureType;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureFingerprint() {
        return failureFingerprint;
    }

    public void setFailureFingerprint(String failureFingerprint) {
        this.failureFingerprint = failureFingerprint;
    }

    public String getSanitizedMessage() {
        return sanitizedMessage;
    }

    public void setSanitizedMessage(String sanitizedMessage) {
        this.sanitizedMessage = sanitizedMessage;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
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

    public String getTlsMode() {
        return tlsMode;
    }

    public void setTlsMode(String tlsMode) {
        this.tlsMode = tlsMode;
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
