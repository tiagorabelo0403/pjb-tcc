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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_recursal_mesh_aggregate")
public class RecursalAggregateState {

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

    @Column(name = "species_name", nullable = false, length = 160)
    private String speciesName;

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

    @Column(name = "preparo_satisfeito", nullable = false)
    private boolean preparoSatisfeito;

    @Column(name = "admissibilidade_positiva", nullable = false)
    private boolean admissibilidadePositiva;

    @Column(name = "remetido", nullable = false)
    private boolean remetido;

    @Column(name = "autuado_destino", nullable = false)
    private boolean autuadoDestino;

    @Column(name = "distribuido_destino", nullable = false)
    private boolean distribuidoDestino;

    @Column(name = "preparo_em_complementacao", nullable = false)
    private boolean preparoEmComplementacao;

    @Column(name = "diligencia_pendente", nullable = false)
    private boolean diligenciaPendente;

    @Column(name = "multa_embargos", nullable = false)
    private boolean multaEmbargos;

    @Column(name = "sobrestado_precedente", nullable = false)
    private boolean sobrestadoPrecedente;

    @Column(name = "efeito_suspensivo_ativo", nullable = false)
    private boolean efeitoSuspensivoAtivo;

    @Column(name = "efeito_ativo_concedido", nullable = false)
    private boolean efeitoAtivoConcedido;

    @Column(name = "conhecimento_parcial", nullable = false)
    private boolean conhecimentoParcial;

    @Column(name = "iteracoes_embargos", nullable = false)
    private int iteracoesEmbargos;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "route_plan_json", nullable = false, columnDefinition = "TEXT")
    private String routePlanJson;

    @Column(name = "context_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson;

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

    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }
    public Processo getProcesso() { return processo; }
    public void setProcesso(Processo processo) { this.processo = processo; }
    public String getNumeroProcesso() { return numeroProcesso; }
    public void setNumeroProcesso(String numeroProcesso) { this.numeroProcesso = numeroProcesso; }
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }
    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }
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
    public boolean isPreparoSatisfeito() { return preparoSatisfeito; }
    public void setPreparoSatisfeito(boolean preparoSatisfeito) { this.preparoSatisfeito = preparoSatisfeito; }
    public boolean isAdmissibilidadePositiva() { return admissibilidadePositiva; }
    public void setAdmissibilidadePositiva(boolean admissibilidadePositiva) { this.admissibilidadePositiva = admissibilidadePositiva; }
    public boolean isRemetido() { return remetido; }
    public void setRemetido(boolean remetido) { this.remetido = remetido; }
    public boolean isAutuadoDestino() { return autuadoDestino; }
    public void setAutuadoDestino(boolean autuadoDestino) { this.autuadoDestino = autuadoDestino; }
    public boolean isDistribuidoDestino() { return distribuidoDestino; }
    public void setDistribuidoDestino(boolean distribuidoDestino) { this.distribuidoDestino = distribuidoDestino; }
    public boolean isPreparoEmComplementacao() { return preparoEmComplementacao; }
    public void setPreparoEmComplementacao(boolean preparoEmComplementacao) { this.preparoEmComplementacao = preparoEmComplementacao; }
    public boolean isDiligenciaPendente() { return diligenciaPendente; }
    public void setDiligenciaPendente(boolean diligenciaPendente) { this.diligenciaPendente = diligenciaPendente; }
    public boolean isMultaEmbargos() { return multaEmbargos; }
    public void setMultaEmbargos(boolean multaEmbargos) { this.multaEmbargos = multaEmbargos; }
    public boolean isSobrestadoPrecedente() { return sobrestadoPrecedente; }
    public void setSobrestadoPrecedente(boolean sobrestadoPrecedente) { this.sobrestadoPrecedente = sobrestadoPrecedente; }
    public boolean isEfeitoSuspensivoAtivo() { return efeitoSuspensivoAtivo; }
    public void setEfeitoSuspensivoAtivo(boolean efeitoSuspensivoAtivo) { this.efeitoSuspensivoAtivo = efeitoSuspensivoAtivo; }
    public boolean isEfeitoAtivoConcedido() { return efeitoAtivoConcedido; }
    public void setEfeitoAtivoConcedido(boolean efeitoAtivoConcedido) { this.efeitoAtivoConcedido = efeitoAtivoConcedido; }
    public boolean isConhecimentoParcial() { return conhecimentoParcial; }
    public void setConhecimentoParcial(boolean conhecimentoParcial) { this.conhecimentoParcial = conhecimentoParcial; }
    public int getIteracoesEmbargos() { return iteracoesEmbargos; }
    public void setIteracoesEmbargos(int iteracoesEmbargos) { this.iteracoesEmbargos = iteracoesEmbargos; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getRoutePlanJson() { return routePlanJson; }
    public void setRoutePlanJson(String routePlanJson) { this.routePlanJson = routePlanJson; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getIntegrityFingerprint() { return integrityFingerprint; }
    public void setIntegrityFingerprint(String integrityFingerprint) { this.integrityFingerprint = integrityFingerprint; }
    public Long getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
