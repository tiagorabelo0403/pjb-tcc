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
@Table(name = "tb_judicial_connector_policy")
public class JudicialConnectorPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_system", nullable = false, length = 40)
    private JudicialSystem connectorSystem;

    @Column(name = "environment_name", length = 60)
    private String environmentName;

    @Column(name = "tribunal_codigo", length = 20)
    private String tribunalCodigo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "production_ready")
    private Boolean productionReady;

    @Column(name = "tribunal_homologated")
    private Boolean tribunalHomologated;

    @Column(name = "tribunal_blocked")
    private Boolean tribunalBlocked;

    @Column(name = "quarantine_enabled")
    private Boolean quarantineEnabled;

    @Column(name = "maintenance_mode")
    private Boolean maintenanceMode;

    @Column(name = "contract_version", length = 80)
    private String contractVersion;

    @Column(name = "certificate_alias", length = 255)
    private String certificateAlias;

    @Column(name = "submit_path", length = 500)
    private String submitPath;

    @Column(name = "dry_run_path", length = 500)
    private String dryRunPath;

    @Column(name = "snapshot_path", length = 500)
    private String snapshotPath;

    @Column(name = "events_path", length = 500)
    private String eventsPath;

    @Column(name = "rollout_state", length = 80)
    private String rolloutState;

    @Column(name = "approved_by", length = 160)
    private String approvedBy;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

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
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public JudicialSystem getConnectorSystem() { return connectorSystem; }
    public void setConnectorSystem(JudicialSystem connectorSystem) { this.connectorSystem = connectorSystem; }
    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public void setTribunalCodigo(String tribunalCodigo) { this.tribunalCodigo = tribunalCodigo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Boolean getProductionReady() { return productionReady; }
    public void setProductionReady(Boolean productionReady) { this.productionReady = productionReady; }
    public Boolean getTribunalHomologated() { return tribunalHomologated; }
    public void setTribunalHomologated(Boolean tribunalHomologated) { this.tribunalHomologated = tribunalHomologated; }
    public Boolean getTribunalBlocked() { return tribunalBlocked; }
    public void setTribunalBlocked(Boolean tribunalBlocked) { this.tribunalBlocked = tribunalBlocked; }
    public Boolean getQuarantineEnabled() { return quarantineEnabled; }
    public void setQuarantineEnabled(Boolean quarantineEnabled) { this.quarantineEnabled = quarantineEnabled; }
    public Boolean getMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(Boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public String getCertificateAlias() { return certificateAlias; }
    public void setCertificateAlias(String certificateAlias) { this.certificateAlias = certificateAlias; }
    public String getSubmitPath() { return submitPath; }
    public void setSubmitPath(String submitPath) { this.submitPath = submitPath; }
    public String getDryRunPath() { return dryRunPath; }
    public void setDryRunPath(String dryRunPath) { this.dryRunPath = dryRunPath; }
    public String getSnapshotPath() { return snapshotPath; }
    public void setSnapshotPath(String snapshotPath) { this.snapshotPath = snapshotPath; }
    public String getEventsPath() { return eventsPath; }
    public void setEventsPath(String eventsPath) { this.eventsPath = eventsPath; }
    public String getRolloutState() { return rolloutState; }
    public void setRolloutState(String rolloutState) { this.rolloutState = rolloutState; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
