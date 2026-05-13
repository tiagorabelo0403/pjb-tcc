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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_judicial_connector_certificate_inventory")
public class JudicialConnectorCertificateInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_system", nullable = false, length = 40)
    private JudicialSystem connectorSystem;

    @Column(name = "tribunal_codigo", length = 20)
    private String tribunalCodigo;

    @Column(name = "environment_name", nullable = false, length = 60)
    private String environmentName;

    @Column(name = "binding_id", nullable = false, length = 120)
    private String bindingId;

    @Column(name = "target_uri", length = 1000)
    private String targetUri;

    @Column(name = "keystore_ref", length = 160)
    private String keyStoreRef;

    @Column(name = "truststore_ref", length = 160)
    private String trustStoreRef;

    @Column(name = "key_alias", length = 255)
    private String keyAlias;

    @Column(name = "tls_mode", length = 20)
    private String tlsMode;

    @Column(name = "certificate_present", nullable = false)
    private boolean certificatePresent;

    @Column(name = "hardware_backed", nullable = false)
    private boolean hardwareBacked;

    @Column(name = "valid_now", nullable = false)
    private boolean validNow;

    @Column(name = "expires_soon", nullable = false)
    private boolean expiresSoon;

    @Column(name = "expired", nullable = false)
    private boolean expired;

    @Column(name = "truststore_present", nullable = false)
    private boolean trustStorePresent;

    @Column(name = "path_validation_succeeded", nullable = false)
    private boolean pathValidationSucceeded;

    @Column(name = "revocation_attempted", nullable = false)
    private boolean revocationAttempted;

    @Column(name = "revocation_soft_failed", nullable = false)
    private boolean revocationSoftFailed;

    @Column(name = "revocation_hard_failed", nullable = false)
    private boolean revocationHardFailed;

    @Column(name = "validation_status", nullable = false, length = 40)
    private String validationStatus;

    @Column(name = "not_before")
    private Instant notBefore;

    @Column(name = "not_after")
    private Instant notAfter;

    @Column(name = "remaining_validity_seconds")
    private Long remainingValiditySeconds;

    @Column(name = "certificate_chain_length", nullable = false)
    private Integer certificateChainLength = 0;

    @Column(name = "subject_dn", length = 1000)
    private String subjectDn;

    @Column(name = "issuer_dn", length = 1000)
    private String issuerDn;

    @Column(name = "serial_number_hex", length = 256)
    private String serialNumberHex;

    @Column(name = "sha256_fingerprint", length = 128)
    private String sha256Fingerprint;

    @Column(name = "blockers_json", columnDefinition = "TEXT")
    private String blockersJson;

    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "last_validated_at", nullable = false)
    private Instant lastValidatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (lastValidatedAt == null) {
            lastValidatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public JudicialSystem getConnectorSystem() { return connectorSystem; }
    public void setConnectorSystem(JudicialSystem connectorSystem) { this.connectorSystem = connectorSystem; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public void setTribunalCodigo(String tribunalCodigo) { this.tribunalCodigo = tribunalCodigo; }
    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }
    public String getBindingId() { return bindingId; }
    public void setBindingId(String bindingId) { this.bindingId = bindingId; }
    public String getTargetUri() { return targetUri; }
    public void setTargetUri(String targetUri) { this.targetUri = targetUri; }
    public String getKeyStoreRef() { return keyStoreRef; }
    public void setKeyStoreRef(String keyStoreRef) { this.keyStoreRef = keyStoreRef; }
    public String getTrustStoreRef() { return trustStoreRef; }
    public void setTrustStoreRef(String trustStoreRef) { this.trustStoreRef = trustStoreRef; }
    public String getKeyAlias() { return keyAlias; }
    public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
    public String getTlsMode() { return tlsMode; }
    public void setTlsMode(String tlsMode) { this.tlsMode = tlsMode; }
    public boolean isCertificatePresent() { return certificatePresent; }
    public void setCertificatePresent(boolean certificatePresent) { this.certificatePresent = certificatePresent; }
    public boolean isHardwareBacked() { return hardwareBacked; }
    public void setHardwareBacked(boolean hardwareBacked) { this.hardwareBacked = hardwareBacked; }
    public boolean isValidNow() { return validNow; }
    public void setValidNow(boolean validNow) { this.validNow = validNow; }
    public boolean isExpiresSoon() { return expiresSoon; }
    public void setExpiresSoon(boolean expiresSoon) { this.expiresSoon = expiresSoon; }
    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }
    public boolean isTrustStorePresent() { return trustStorePresent; }
    public void setTrustStorePresent(boolean trustStorePresent) { this.trustStorePresent = trustStorePresent; }
    public boolean isPathValidationSucceeded() { return pathValidationSucceeded; }
    public void setPathValidationSucceeded(boolean pathValidationSucceeded) { this.pathValidationSucceeded = pathValidationSucceeded; }
    public boolean isRevocationAttempted() { return revocationAttempted; }
    public void setRevocationAttempted(boolean revocationAttempted) { this.revocationAttempted = revocationAttempted; }
    public boolean isRevocationSoftFailed() { return revocationSoftFailed; }
    public void setRevocationSoftFailed(boolean revocationSoftFailed) { this.revocationSoftFailed = revocationSoftFailed; }
    public boolean isRevocationHardFailed() { return revocationHardFailed; }
    public void setRevocationHardFailed(boolean revocationHardFailed) { this.revocationHardFailed = revocationHardFailed; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Instant getNotBefore() { return notBefore; }
    public void setNotBefore(Instant notBefore) { this.notBefore = notBefore; }
    public Instant getNotAfter() { return notAfter; }
    public void setNotAfter(Instant notAfter) { this.notAfter = notAfter; }
    public Long getRemainingValiditySeconds() { return remainingValiditySeconds; }
    public void setRemainingValiditySeconds(Long remainingValiditySeconds) { this.remainingValiditySeconds = remainingValiditySeconds; }
    public Integer getCertificateChainLength() { return certificateChainLength; }
    public void setCertificateChainLength(Integer certificateChainLength) { this.certificateChainLength = certificateChainLength; }
    public String getSubjectDn() { return subjectDn; }
    public void setSubjectDn(String subjectDn) { this.subjectDn = subjectDn; }
    public String getIssuerDn() { return issuerDn; }
    public void setIssuerDn(String issuerDn) { this.issuerDn = issuerDn; }
    public String getSerialNumberHex() { return serialNumberHex; }
    public void setSerialNumberHex(String serialNumberHex) { this.serialNumberHex = serialNumberHex; }
    public String getSha256Fingerprint() { return sha256Fingerprint; }
    public void setSha256Fingerprint(String sha256Fingerprint) { this.sha256Fingerprint = sha256Fingerprint; }
    public String getBlockersJson() { return blockersJson; }
    public void setBlockersJson(String blockersJson) { this.blockersJson = blockersJson; }
    public String getWarningsJson() { return warningsJson; }
    public void setWarningsJson(String warningsJson) { this.warningsJson = warningsJson; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Instant getLastValidatedAt() { return lastValidatedAt; }
    public void setLastValidatedAt(Instant lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
