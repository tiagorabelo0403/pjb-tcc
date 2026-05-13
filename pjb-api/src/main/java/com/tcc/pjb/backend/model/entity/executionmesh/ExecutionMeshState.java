package com.tcc.pjb.backend.model.entity.executionmesh;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.model.entity.Processo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_execution_mesh_state")
public class ExecutionMeshState {

    @Id
    @Column(name = "aggregate_id", nullable = false, length = 80)
    private String aggregateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "numero_processo", length = 50)
    private String numeroProcesso;

    @Column(name = "species_code", length = 80, nullable = false)
    private String speciesCode;

    @Column(name = "current_stage", length = 80, nullable = false)
    private String currentStage;

    @Column(name = "current_queue", length = 120)
    private String currentQueue;

    @Column(name = "current_inbox", length = 120)
    private String currentInbox;

    @Column(name = "current_impact", length = 120)
    private String currentImpact;

    @Column(name = "current_asset_kind", length = 80)
    private String currentAssetKind;

    @Column(name = "current_gateway", length = 120)
    private String currentGateway;

    @Column(name = "external_status", length = 80)
    private String externalStatus;

    @Column(name = "terminal_disposition", length = 120)
    private String terminalDisposition;

    @Column(name = "current_expropriation_mode", length = 120)
    private String currentExpropriationMode;

    @Column(name = "current_auction_cycle_mode", length = 140)
    private String currentAuctionCycleMode;

    @Column(name = "current_contingency_mode", length = 140)
    private String currentContingencyMode;

    @Column(name = "current_homologation_mode", length = 160)
    private String currentHomologationMode;

    @Column(name = "current_closure_mode", length = 160)
    private String currentClosureMode;

    @Column(name = "closure_consistency_status", length = 160)
    private String closureConsistencyStatus;

    @Column(name = "current_preference_mode", length = 160)
    private String currentPreferenceMode;

    @Column(name = "subrogation_status", length = 160)
    private String subrogationStatus;

    @Column(name = "reconciliation_status", length = 120)
    private String reconciliationStatus;

    @Column(name = "archive_link_status", length = 120)
    private String archiveLinkStatus;

    @Column(name = "satisfaction_state", length = 120)
    private String satisfactionState;

    @Column(name = "satisfaction_percent", precision = 7, scale = 4)
    private BigDecimal satisfactionPercent;

    @Column(name = "residual_amount", precision = 19, scale = 2)
    private BigDecimal residualAmount;

    @Column(name = "incident_count", nullable = false)
    private int incidentCount;

    @Column(name = "enforcement_count", nullable = false)
    private int enforcementCount;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "incident_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String incidentLedgerJson;

    @Column(name = "enforcement_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String enforcementLedgerJson;

    @Column(name = "patrimonial_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String patrimonialLedgerJson;

    @Column(name = "external_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String externalLedgerJson;

    @Column(name = "terminal_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String terminalLedgerJson;

    @Column(name = "expropriation_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String expropriationLedgerJson;

    @Column(name = "auction_cycle_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String auctionCycleLedgerJson;

    @Column(name = "contingency_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String contingencyLedgerJson;

    @Column(name = "reconciliation_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String reconciliationLedgerJson;

    @Column(name = "archive_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String archiveLedgerJson;

    @Column(name = "homologation_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String homologationLedgerJson;

    @Column(name = "settlement_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String settlementLedgerJson;

    @Column(name = "closure_ledger_json", nullable = false, columnDefinition = "TEXT")
    private String closureLedgerJson;

    @Column(name = "integrity_fingerprint", nullable = false, length = 64)
    private String integrityFingerprint;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public Processo getProcesso() { return processo; }
    public void setProcesso(Processo processo) { this.processo = processo; }
    public String getNumeroProcesso() { return numeroProcesso; }
    public void setNumeroProcesso(String numeroProcesso) { this.numeroProcesso = numeroProcesso; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public String getCurrentQueue() { return currentQueue; }
    public void setCurrentQueue(String currentQueue) { this.currentQueue = currentQueue; }
    public String getCurrentInbox() { return currentInbox; }
    public void setCurrentInbox(String currentInbox) { this.currentInbox = currentInbox; }
    public String getCurrentImpact() { return currentImpact; }
    public void setCurrentImpact(String currentImpact) { this.currentImpact = currentImpact; }
    public String getCurrentAssetKind() { return currentAssetKind; }
    public void setCurrentAssetKind(String currentAssetKind) { this.currentAssetKind = currentAssetKind; }
    public String getCurrentGateway() { return currentGateway; }
    public void setCurrentGateway(String currentGateway) { this.currentGateway = currentGateway; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public String getTerminalDisposition() { return terminalDisposition; }
    public void setTerminalDisposition(String terminalDisposition) { this.terminalDisposition = terminalDisposition; }
    public String getCurrentExpropriationMode() { return currentExpropriationMode; }
    public void setCurrentExpropriationMode(String currentExpropriationMode) { this.currentExpropriationMode = currentExpropriationMode; }
    public String getCurrentAuctionCycleMode() { return currentAuctionCycleMode; }
    public void setCurrentAuctionCycleMode(String currentAuctionCycleMode) { this.currentAuctionCycleMode = currentAuctionCycleMode; }
    public String getCurrentContingencyMode() { return currentContingencyMode; }
    public void setCurrentContingencyMode(String currentContingencyMode) { this.currentContingencyMode = currentContingencyMode; }
    public String getCurrentHomologationMode() { return currentHomologationMode; }
    public void setCurrentHomologationMode(String currentHomologationMode) { this.currentHomologationMode = currentHomologationMode; }
    public String getCurrentClosureMode() { return currentClosureMode; }
    public void setCurrentClosureMode(String currentClosureMode) { this.currentClosureMode = currentClosureMode; }
    public String getClosureConsistencyStatus() { return closureConsistencyStatus; }
    public void setClosureConsistencyStatus(String closureConsistencyStatus) { this.closureConsistencyStatus = closureConsistencyStatus; }
    public String getCurrentPreferenceMode() { return currentPreferenceMode; }
    public void setCurrentPreferenceMode(String currentPreferenceMode) { this.currentPreferenceMode = currentPreferenceMode; }
    public String getSubrogationStatus() { return subrogationStatus; }
    public void setSubrogationStatus(String subrogationStatus) { this.subrogationStatus = subrogationStatus; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getArchiveLinkStatus() { return archiveLinkStatus; }
    public void setArchiveLinkStatus(String archiveLinkStatus) { this.archiveLinkStatus = archiveLinkStatus; }
    public String getSatisfactionState() { return satisfactionState; }
    public void setSatisfactionState(String satisfactionState) { this.satisfactionState = satisfactionState; }
    public BigDecimal getSatisfactionPercent() { return satisfactionPercent; }
    public void setSatisfactionPercent(BigDecimal satisfactionPercent) { this.satisfactionPercent = satisfactionPercent; }
    public BigDecimal getResidualAmount() { return residualAmount; }
    public void setResidualAmount(BigDecimal residualAmount) { this.residualAmount = residualAmount; }
    public int getIncidentCount() { return incidentCount; }
    public void setIncidentCount(int incidentCount) { this.incidentCount = incidentCount; }
    public int getEnforcementCount() { return enforcementCount; }
    public void setEnforcementCount(int enforcementCount) { this.enforcementCount = enforcementCount; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getIncidentLedgerJson() { return incidentLedgerJson; }
    public void setIncidentLedgerJson(String incidentLedgerJson) { this.incidentLedgerJson = incidentLedgerJson; }
    public String getEnforcementLedgerJson() { return enforcementLedgerJson; }
    public void setEnforcementLedgerJson(String enforcementLedgerJson) { this.enforcementLedgerJson = enforcementLedgerJson; }
    public String getPatrimonialLedgerJson() { return patrimonialLedgerJson; }
    public void setPatrimonialLedgerJson(String patrimonialLedgerJson) { this.patrimonialLedgerJson = patrimonialLedgerJson; }
    public String getExternalLedgerJson() { return externalLedgerJson; }
    public void setExternalLedgerJson(String externalLedgerJson) { this.externalLedgerJson = externalLedgerJson; }
    public String getTerminalLedgerJson() { return terminalLedgerJson; }
    public void setTerminalLedgerJson(String terminalLedgerJson) { this.terminalLedgerJson = terminalLedgerJson; }
    public String getExpropriationLedgerJson() { return expropriationLedgerJson; }
    public void setExpropriationLedgerJson(String expropriationLedgerJson) { this.expropriationLedgerJson = expropriationLedgerJson; }
    public String getAuctionCycleLedgerJson() { return auctionCycleLedgerJson; }
    public void setAuctionCycleLedgerJson(String auctionCycleLedgerJson) { this.auctionCycleLedgerJson = auctionCycleLedgerJson; }
    public String getContingencyLedgerJson() { return contingencyLedgerJson; }
    public void setContingencyLedgerJson(String contingencyLedgerJson) { this.contingencyLedgerJson = contingencyLedgerJson; }
    public String getReconciliationLedgerJson() { return reconciliationLedgerJson; }
    public void setReconciliationLedgerJson(String reconciliationLedgerJson) { this.reconciliationLedgerJson = reconciliationLedgerJson; }
    public String getArchiveLedgerJson() { return archiveLedgerJson; }
    public void setArchiveLedgerJson(String archiveLedgerJson) { this.archiveLedgerJson = archiveLedgerJson; }
    public String getHomologationLedgerJson() { return homologationLedgerJson; }
    public void setHomologationLedgerJson(String homologationLedgerJson) { this.homologationLedgerJson = homologationLedgerJson; }
    public String getSettlementLedgerJson() { return settlementLedgerJson; }
    public void setSettlementLedgerJson(String settlementLedgerJson) { this.settlementLedgerJson = settlementLedgerJson; }
    public String getClosureLedgerJson() { return closureLedgerJson; }
    public void setClosureLedgerJson(String closureLedgerJson) { this.closureLedgerJson = closureLedgerJson; }
    public String getIntegrityFingerprint() { return integrityFingerprint; }
    public void setIntegrityFingerprint(String integrityFingerprint) { this.integrityFingerprint = integrityFingerprint; }
    public Long getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
