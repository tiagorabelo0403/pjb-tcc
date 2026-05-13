package com.tcc.pjb.backend.model.entity.recursalmesh;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_recursal_mesh_process_projection")
public class RecursalProcessIntegrationState {

    @Id
    @Column(name = "recurso_id", nullable = false, length = 160)
    private String recursoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "numero_processo", length = 50)
    private String numeroProcesso;

    @Column(name = "species_code", nullable = false, length = 30)
    private String speciesCode;

    @Column(name = "profile_name", nullable = false, length = 120)
    private String profileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 40)
    private RecursalLifecycleState currentState;

    @Enumerated(EnumType.STRING)
    @Column(name = "tribunal_atual", nullable = false, length = 20)
    private RecursalTribunal tribunalAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "tribunal_detalhado_atual", nullable = false, length = 20)
    private RecursalTribunalDetalhado tribunalDetalhadoAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "instancia_atual", nullable = false, length = 20)
    private InstanceLevel instanciaAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "autoridade_atual", nullable = false, length = 30)
    private RecursalAuthority autoridadeAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_event", length = 40)
    private RecursalTransitionEvent lastEvent;

    @Column(name = "current_revision", nullable = false)
    private int currentRevision;

    @Column(name = "total_transitions", nullable = false)
    private int totalTransitions;

    @Column(name = "iteracoes_embargos", nullable = false)
    private int iteracoesEmbargos;

    @Column(name = "transitado_em_julgado", nullable = false)
    private boolean transitadoEmJulgado;

    @Column(name = "last_actor", length = 160)
    private String lastActor;

    @Column(name = "last_transition_at")
    private Instant lastTransitionAt;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "route_plan_json", nullable = false, columnDefinition = "TEXT")
    private String routePlanJson;

    @Column(name = "integrity_fingerprint", nullable = false, length = 64)
    private String integrityFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }
    public Processo getProcesso() { return processo; }
    public void setProcesso(Processo processo) { this.processo = processo; }
    public String getNumeroProcesso() { return numeroProcesso; }
    public void setNumeroProcesso(String numeroProcesso) { this.numeroProcesso = numeroProcesso; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public RecursalLifecycleState getCurrentState() { return currentState; }
    public void setCurrentState(RecursalLifecycleState currentState) { this.currentState = currentState; }
    public RecursalTribunal getTribunalAtual() { return tribunalAtual; }
    public void setTribunalAtual(RecursalTribunal tribunalAtual) { this.tribunalAtual = tribunalAtual; }
    public RecursalTribunalDetalhado getTribunalDetalhadoAtual() { return tribunalDetalhadoAtual; }
    public void setTribunalDetalhadoAtual(RecursalTribunalDetalhado tribunalDetalhadoAtual) { this.tribunalDetalhadoAtual = tribunalDetalhadoAtual; }
    public InstanceLevel getInstanciaAtual() { return instanciaAtual; }
    public void setInstanciaAtual(InstanceLevel instanciaAtual) { this.instanciaAtual = instanciaAtual; }
    public RecursalAuthority getAutoridadeAtual() { return autoridadeAtual; }
    public void setAutoridadeAtual(RecursalAuthority autoridadeAtual) { this.autoridadeAtual = autoridadeAtual; }
    public RecursalTransitionEvent getLastEvent() { return lastEvent; }
    public void setLastEvent(RecursalTransitionEvent lastEvent) { this.lastEvent = lastEvent; }
    public int getCurrentRevision() { return currentRevision; }
    public void setCurrentRevision(int currentRevision) { this.currentRevision = currentRevision; }
    public int getTotalTransitions() { return totalTransitions; }
    public void setTotalTransitions(int totalTransitions) { this.totalTransitions = totalTransitions; }
    public int getIteracoesEmbargos() { return iteracoesEmbargos; }
    public void setIteracoesEmbargos(int iteracoesEmbargos) { this.iteracoesEmbargos = iteracoesEmbargos; }
    public boolean isTransitadoEmJulgado() { return transitadoEmJulgado; }
    public void setTransitadoEmJulgado(boolean transitadoEmJulgado) { this.transitadoEmJulgado = transitadoEmJulgado; }
    public String getLastActor() { return lastActor; }
    public void setLastActor(String lastActor) { this.lastActor = lastActor; }
    public Instant getLastTransitionAt() { return lastTransitionAt; }
    public void setLastTransitionAt(Instant lastTransitionAt) { this.lastTransitionAt = lastTransitionAt; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getRoutePlanJson() { return routePlanJson; }
    public void setRoutePlanJson(String routePlanJson) { this.routePlanJson = routePlanJson; }
    public String getIntegrityFingerprint() { return integrityFingerprint; }
    public void setIntegrityFingerprint(String integrityFingerprint) { this.integrityFingerprint = integrityFingerprint; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
