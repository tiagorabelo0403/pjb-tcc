package com.tcc.pjb.backend.model.entity.recursalmesh;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_recursal_mesh_transition_ledger", indexes = {
        @Index(name = "idx_recursal_mesh_ledger_recurso_revision", columnList = "recurso_id, to_revision"),
        @Index(name = "idx_recursal_mesh_ledger_recurso_command", columnList = "recurso_id, command_id", unique = true),
        @Index(name = "idx_recursal_mesh_ledger_occurred_at", columnList = "occurred_at")
})
public class RecursalTransitionLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recurso_id", nullable = false, length = 160)
    private String recursoId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "species_code", nullable = false, length = 30)
    private String speciesCode;

    @Column(name = "profile_name", nullable = false, length = 120)
    private String profileName;

    @Column(name = "command_id", length = 160)
    private String commandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_code", nullable = false, length = 40)
    private RecursalTransitionEvent eventCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 40)
    private RecursalLifecycleState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 40)
    private RecursalLifecycleState toState;

    @Column(name = "from_revision", nullable = false)
    private int fromRevision;

    @Column(name = "to_revision", nullable = false)
    private int toRevision;

    @Column(name = "actor", length = 160)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "route_plan_json", nullable = false, columnDefinition = "TEXT")
    private String routePlanJson;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "integrity_fingerprint", nullable = false, length = 64)
    private String integrityFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }
    public Long getProcessoId() { return processoId; }
    public void setProcessoId(Long processoId) { this.processoId = processoId; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public RecursalTransitionEvent getEventCode() { return eventCode; }
    public void setEventCode(RecursalTransitionEvent eventCode) { this.eventCode = eventCode; }
    public RecursalLifecycleState getFromState() { return fromState; }
    public void setFromState(RecursalLifecycleState fromState) { this.fromState = fromState; }
    public RecursalLifecycleState getToState() { return toState; }
    public void setToState(RecursalLifecycleState toState) { this.toState = toState; }
    public int getFromRevision() { return fromRevision; }
    public void setFromRevision(int fromRevision) { this.fromRevision = fromRevision; }
    public int getToRevision() { return toRevision; }
    public void setToRevision(int toRevision) { this.toRevision = toRevision; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getRoutePlanJson() { return routePlanJson; }
    public void setRoutePlanJson(String routePlanJson) { this.routePlanJson = routePlanJson; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getIntegrityFingerprint() { return integrityFingerprint; }
    public void setIntegrityFingerprint(String integrityFingerprint) { this.integrityFingerprint = integrityFingerprint; }
    public Instant getCreatedAt() { return createdAt; }
}
