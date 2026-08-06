package com.tcc.pjb.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.ModuloProcesso;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;

@Entity
@FilterDef(
        name = "filtroEquipeProcesso",
        parameters = {
                @ParamDef(name = "usuarioIdParam", type = Long.class),
                @ParamDef(name = "equipeIdParam", type = Long.class),
                @ParamDef(name = "includePersonalParam", type = Boolean.class),
                @ParamDef(name = "allRamosParam", type = Boolean.class),
                @ParamDef(name = "allowSensitiveParam", type = Boolean.class),
                @ParamDef(name = "blockPersonalCasesParam", type = Boolean.class),
                @ParamDef(name = "userCpfParam", type = String.class),
                @ParamDef(name = "allowedRamosParam", type = String.class)
        }
)
@Filter(
        name = "filtroEquipeProcesso",
        condition = "((( :equipeIdParam = -1 and equipe_id is null and usuario_id = :usuarioIdParam ) " +
                "or ( :equipeIdParam <> -1 and equipe_id = :equipeIdParam ) " +
                "or ( :includePersonalParam = true and equipe_id is null and usuario_id = :usuarioIdParam )) " +
                "and ( :allRamosParam = true or ramo_direito is null or ramo_direito in (:allowedRamosParam) ) " +
                "and ( :allowSensitiveParam = true or nivel_sigilo is null or nivel_sigilo = 'PUBLICO' ) " +
                "and ( :blockPersonalCasesParam = false or (((parte_autora_cpf is null or parte_autora_cpf <> :userCpfParam) and (parte_reu_cpf is null or parte_reu_cpf <> :userCpfParam)) and (usuario_id is null or usuario_id <> :usuarioIdParam)))"
)
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Table(name = "tb_processo")
@JsonIgnoreProperties({"usuario", "equipe", "proceduralRouting", "submissionBlueprint", "connectorExecution", "gapsEstrategicos", "planoEstrutural"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @Column(name = "numero_processo")
    private String numeroProcesso;

    @Column(name = "numero_unificado")
    private String numeroUnificado;

    private String tribunal;
    private String vara;
    private String comarca;
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_direito")
    private RamoDireito ramoDireito;

    @Enumerated(EnumType.STRING)
    private RitoProcessual rito;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_atual")
    private FaseProcessual faseAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processo")
    private StatusProcesso statusProcesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "materia")
    private MateriaJurisdicao materia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_justica")
    private TipoJustica tipoJustica;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo")
    private NivelSigilo nivelSigilo;

    @Enumerated(EnumType.STRING)
    private ModuloProcesso modulo;

    @Column(name = "classe_processual")
    private String classeProcessual;

    @Column(name = "classe_tpu_codigo")
    private String classeTpuCodigo;

    private String assunto;

    @Column(name = "objeto_processual", length = 12000)
    private String objetoProcessual;

    @Column(name = "pedido_principal", length = 12000)
    private String pedidoPrincipal;

    @Column(name = "pedidos_consolidados", length = 20000)
    private String pedidosConsolidados;

    @Column(name = "material_probatorio_resumo", length = 20000)
    private String materialProbatorioResumo;

    @Column(name = "material_probatorio_hash", length = 128)
    private String materialProbatorioHash;

    @Column(name = "resumo_ia", length = 20000)
    private String resumoIA;

    @Column(name = "resultado_final", length = 20000)
    private String resultadoFinal;

    @Column(name = "peticao_inicial_text", length = 30000)
    private String peticaoInicialText;

    @Column(name = "analise_triagem_v1", length = 12000)
    private String analiseTriagemV1;

    @Column(name = "janela_acordo_resumo", length = 4000)
    private String janelaAcordoResumo;

    @Column(name = "valor_causa", precision = 19, scale = 2)
    private BigDecimal valorCausa;

    @Column(name = "routing_confidence", precision = 10, scale = 4)
    private BigDecimal routingConfidence;

    @Column(name = "material_probatorio_score")
    private Integer materialProbatorioScore;

    @Column(name = "potencial_acordo_score")
    private Integer potencialAcordoScore;

    @Column(name = "score_complexidade")
    private Integer scoreComplexidade;

    @Column(name = "parte_autora_cpf")
    private String parteAutoraCpf;

    @Column(name = "parte_reu_cpf")
    private String parteReuCpf;

    @Column(name = "uf_autor", length = 2)
    private String ufAutor;

    @Column(name = "comarca_autor", length = 120)
    private String comarcaAutor;

    @Column(name = "cidade_autor", length = 120)
    private String cidadeAutor;

    @Column(name = "uf_reu", length = 2)
    private String ufReu;

    @Column(name = "comarca_reu", length = 120)
    private String comarcaReu;

    @Column(name = "cidade_reu", length = 120)
    private String cidadeReu;

    @Column(name = "parte_autora_nome")
    private String parteAutoraNome;

    @Column(name = "parte_reu_nome")
    private String parteReuNome;

    @Column(name = "prevention_mode")
    private String preventionMode;

    @Column(name = "linkage_mode")
    private String linkageMode;

    @Column(name = "routing_risk_level")
    private String routingRiskLevel;

    @Column(name = "submission_blueprint_status")
    private String submissionBlueprintStatus;

    @Column(name = "submission_dry_run_status")
    private String submissionDryRunStatus;

    @Column(name = "pre_protocolo_status")
    private String preProtocoloStatus;

    @Column(name = "competencia_territorial_modo")
    private String competenciaTerritorialModo;

    @Column(name = "connector_system")
    private String connectorSystem;

    @Column(name = "connector_submission_status")
    private String connectorSubmissionStatus;

    @Column(name = "instrumento_representacao_resolvido")
    private String instrumentoRepresentacaoResolvido;

    @Column(name = "connector_protocol_reference")
    private String connectorProtocolReference;

    @Column(name = "connector_client_id")
    private String connectorClientId;

    @Column(name = "connector_submission_message", length = 4000)
    private String connectorSubmissionMessage;

    @Column(name = "connector_submission_processed_at")
    private LocalDateTime connectorSubmissionProcessedAt;

    @Column(name = "connector_submission_attempts")
    private Integer connectorSubmissionAttempts;

    @Column(name = "connector_last_submission_attempt_at")
    private LocalDateTime connectorLastSubmissionAttemptAt;

    @Column(name = "connector_sync_status")
    private String connectorSyncStatus;

    @Column(name = "connector_sync_message", length = 4000)
    private String connectorSyncMessage;

    @Column(name = "connector_snapshot_synced_at")
    private LocalDateTime connectorSnapshotSyncedAt;

    @Column(name = "connector_events_synced_at")
    private LocalDateTime connectorEventsSyncedAt;

    @Column(name = "connector_sync_attempts")
    private Integer connectorSyncAttempts;

    @Column(name = "tribunal_codigo_roteado")
    private String tribunalCodigoRoteado;

    @Column(name = "unidade_judiciaria_codigo")
    private String unidadeJudiciariaCodigo;

    @Column(name = "data_distribuicao")
    private LocalDateTime dataDistribuicao;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "data_ultima_movimentacao")
    private LocalDateTime dataUltimaMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jurisdicao_id")
    private Jurisdicao jurisdicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Transient
    private Long catalogVersionId;

    @Transient
    private ProceduralRoutingReport proceduralRouting;

    @Transient
    private ProceduralSubmissionBlueprintReport submissionBlueprint;

    @Transient
    private ProceduralConnectorExecutionReport connectorExecution;

    @Transient
    @Builder.Default
    private List<String> gapsEstrategicos = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<String> planoEstrutural = new ArrayList<>();

    public String getNumero() {
        return numero == null || numero.isBlank() ? numeroProcesso : numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
        if ((this.numeroProcesso == null || this.numeroProcesso.isBlank()) && numero != null && !numero.isBlank()) {
            this.numeroProcesso = numero;
        }
    }

    public String getNumeroProcesso() {
        return numeroProcesso == null || numeroProcesso.isBlank() ? numero : numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
        if ((this.numero == null || this.numero.isBlank()) && numeroProcesso != null && !numeroProcesso.isBlank()) {
            this.numero = numeroProcesso;
        }
    }

    public String getNumeroCNJ() {
        return numeroUnificado == null || numeroUnificado.isBlank() ? getNumeroProcesso() : numeroUnificado;
    }

    public void setNumeroCNJ(String numeroCNJ) {
        this.numeroUnificado = numeroCNJ;
    }

    public RamoDireito getRamo() {
        return ramoDireito;
    }

    public void setRamo(RamoDireito ramo) {
        this.ramoDireito = ramo;
    }

    public NivelSigilo getSigilo() {
        return nivelSigilo;
    }

    public void setSigilo(NivelSigilo sigilo) {
        this.nivelSigilo = sigilo;
    }

    public StatusProcesso getStatus() {
        return statusProcesso;
    }

    public void setStatus(StatusProcesso status) {
        this.statusProcesso = status;
    }

    public void setStatusProcesso(String status) {
        this.statusProcesso = StatusProcesso.fromString(status);
    }

    public void setFaseAtual(String faseAtual) {
        this.faseAtual = FaseProcessual.fromString(faseAtual).orElse(null);
    }

    public FaseProcessual getFaseProcessualNormalizada() {
        return faseAtual;
    }

    public void setTipoJustica(String tipoJustica) {
        this.tipoJustica = TipoJustica.fromString(tipoJustica);
    }

    public void setConnectorSystem(JudicialSystem connectorSystem) {
        this.connectorSystem = connectorSystem == null ? null : connectorSystem.name();
    }

    public void setDataUltimaMovimentacao(Instant dataUltimaMovimentacao) {
        this.dataUltimaMovimentacao = dataUltimaMovimentacao == null ? null : LocalDateTime.ofInstant(dataUltimaMovimentacao, ZoneOffset.UTC);
    }

    public void setDataUltimaMovimentacao(LocalDateTime dataUltimaMovimentacao) {
        this.dataUltimaMovimentacao = dataUltimaMovimentacao;
    }

    public void setDataDistribuicao(Instant dataDistribuicao) {
        this.dataDistribuicao = dataDistribuicao == null ? null : LocalDateTime.ofInstant(dataDistribuicao, ZoneOffset.UTC);
    }

    public void setDataCriacao(Instant dataCriacao) {
        this.dataCriacao = dataCriacao == null ? null : LocalDateTime.ofInstant(dataCriacao, ZoneOffset.UTC);
    }

    public boolean isSigiloso() {
        return nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO;
    }

    public String getNomesPoloAtivo() {
        return parteAutoraNome;
    }

    public String getPoloAtivoTipo() {
        return parteAutoraCpf != null && !parteAutoraCpf.isBlank() ? "PESSOA_FISICA" : "PESSOA_JURIDICA";
    }

    public String getNomesPoloPassivo() {
        return parteReuNome;
    }

    public String getPeticaoInicialText() {
        if (peticaoInicialText != null && !peticaoInicialText.isBlank()) {
            return peticaoInicialText;
        }
        StringBuilder out = new StringBuilder();
        appendLine(out, pedidoPrincipal);
        appendLine(out, pedidosConsolidados);
        appendLine(out, resumoIA);
        appendLine(out, objetoProcessual);
        return out.isEmpty() ? null : out.toString();
    }

    private static void appendLine(StringBuilder out, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(System.lineSeparator());
        }
        out.append(value.trim());
    }

    public FaseProcessual getFaseAtual() {
        return faseAtual;
    }

    public StatusProcesso getStatusProcesso() {
        return statusProcesso;
    }

    public Jurisdicao getJurisdicao() {
        return jurisdicao;
    }

    public TipoJustica getTipoJustica() {
        return tipoJustica;
    }

    public MateriaJurisdicao getMateria() {
        return materia;
    }

    public String getResumoIA() {
        return resumoIA;
    }

    public void setResumoIA(String resumoIA) {
        this.resumoIA = resumoIA;
    }

    public String getPedidosConsolidados() {
        return pedidosConsolidados;
    }

    public String getResultadoFinal() {
        return resultadoFinal;
    }

    public String getCompetenciaTerritorialModo() {
        return competenciaTerritorialModo;
    }

    public String getConnectorSystem() {
        return connectorSystem;
    }

    public String getConnectorTargetSystem() {
        return connectorSystem;
    }

    public String getConnectorSubmissionMessage() {
        return connectorSubmissionMessage;
    }

    public void setConnectorSubmissionMessage(String connectorSubmissionMessage) {
        this.connectorSubmissionMessage = limitText(connectorSubmissionMessage, 500);
    }

    public String getTribunalCodigoRoteado() {
        return tribunalCodigoRoteado;
    }

    public void setTribunalCodigoRoteado(String tribunalCodigoRoteado) {
        this.tribunalCodigoRoteado = tribunalCodigoRoteado;
    }

    public BigDecimal getRoutingConfidence() {
        return routingConfidence;
    }

    public Integer getMaterialProbatorioScore() {
        return materialProbatorioScore;
    }

    public String getMaterialProbatorioHash() {
        return materialProbatorioHash;
    }

    public String getPedidoPrincipal() {
        return pedidoPrincipal;
    }

    public Integer getScoreComplexidade() {
        return scoreComplexidade;
    }

    public ModuloProcesso getModulo() {
        return modulo;
    }

    public LocalDateTime getDataUltimaMovimentacao() {
        return dataUltimaMovimentacao;
    }


    public void setStatusProcesso(StatusProcesso statusProcesso) {
        this.statusProcesso = statusProcesso;
    }

    public void setFaseAtual(FaseProcessual faseAtual) {
        this.faseAtual = faseAtual;
    }

    public void setTipoJustica(TipoJustica tipoJustica) {
        this.tipoJustica = tipoJustica;
    }

    public void setConnectorSystem(String connectorSystem) {
        this.connectorSystem = connectorSystem;
    }

    public void setConnectorTargetSystem(String connectorTargetSystem) {
        this.connectorSystem = connectorTargetSystem;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataDistribuicao() {
        return dataDistribuicao;
    }

    public void setDataDistribuicao(LocalDateTime dataDistribuicao) {
        this.dataDistribuicao = dataDistribuicao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public void setResultadoFinal(String resultadoFinal) {
        this.resultadoFinal = resultadoFinal;
    }



    public void setJurisdicao(Jurisdicao jurisdicao) {
        this.jurisdicao = jurisdicao;
    }

    public void setPedidoPrincipal(String pedidoPrincipal) {
        this.pedidoPrincipal = pedidoPrincipal;
    }

    public void setPedidosConsolidados(String pedidosConsolidados) {
        this.pedidosConsolidados = pedidosConsolidados;
    }

    public void setMaterialProbatorioHash(String materialProbatorioHash) {
        this.materialProbatorioHash = materialProbatorioHash;
    }

    public void setMaterialProbatorioScore(Integer materialProbatorioScore) {
        this.materialProbatorioScore = materialProbatorioScore;
    }

    public Integer getPotencialAcordoScore() {
        return potencialAcordoScore;
    }

    public void setPotencialAcordoScore(Integer potencialAcordoScore) {
        this.potencialAcordoScore = potencialAcordoScore;
    }

    public String getJanelaAcordoResumo() {
        return janelaAcordoResumo;
    }

    public void setJanelaAcordoResumo(String janelaAcordoResumo) {
        this.janelaAcordoResumo = janelaAcordoResumo;
    }

    public String getMaterialProbatorioResumo() {
        return materialProbatorioResumo;
    }

    public void setMaterialProbatorioResumo(String materialProbatorioResumo) {
        this.materialProbatorioResumo = materialProbatorioResumo;
    }

    public String getClasseTpuCodigo() {
        return classeTpuCodigo;
    }

    public void setClasseTpuCodigo(String classeTpuCodigo) {
        this.classeTpuCodigo = classeTpuCodigo;
    }

    public String getConnectorProtocolReference() {
        return connectorProtocolReference;
    }

    public void setConnectorProtocolReference(String connectorProtocolReference) {
        this.connectorProtocolReference = connectorProtocolReference;
    }

    public String getConnectorSubmissionStatus() {
        return connectorSubmissionStatus;
    }

    public void setConnectorSubmissionStatus(String connectorSubmissionStatus) {
        this.connectorSubmissionStatus = connectorSubmissionStatus;
    }

    public String getConnectorSyncStatus() {
        return connectorSyncStatus;
    }

    public void setConnectorSyncStatus(String connectorSyncStatus) {
        this.connectorSyncStatus = connectorSyncStatus;
    }

    public String getConnectorSyncMessage() {
        return connectorSyncMessage;
    }

    public void setConnectorSyncMessage(String connectorSyncMessage) {
        this.connectorSyncMessage = limitText(connectorSyncMessage, 500);
    }

    public LocalDateTime getConnectorSubmissionProcessedAt() {
        return connectorSubmissionProcessedAt;
    }

    public void setConnectorSubmissionProcessedAt(LocalDateTime connectorSubmissionProcessedAt) {
        this.connectorSubmissionProcessedAt = connectorSubmissionProcessedAt;
    }

    public Integer getConnectorSubmissionAttempts() {
        return connectorSubmissionAttempts;
    }

    public void setConnectorSubmissionAttempts(Integer connectorSubmissionAttempts) {
        this.connectorSubmissionAttempts = connectorSubmissionAttempts;
    }

    public LocalDateTime getConnectorLastSubmissionAttemptAt() {
        return connectorLastSubmissionAttemptAt;
    }

    public void setConnectorLastSubmissionAttemptAt(LocalDateTime connectorLastSubmissionAttemptAt) {
        this.connectorLastSubmissionAttemptAt = connectorLastSubmissionAttemptAt;
    }

    public LocalDateTime getConnectorSnapshotSyncedAt() {
        return connectorSnapshotSyncedAt;
    }

    public void setConnectorSnapshotSyncedAt(LocalDateTime connectorSnapshotSyncedAt) {
        this.connectorSnapshotSyncedAt = connectorSnapshotSyncedAt;
    }

    public LocalDateTime getConnectorEventsSyncedAt() {
        return connectorEventsSyncedAt;
    }

    public void setConnectorEventsSyncedAt(LocalDateTime connectorEventsSyncedAt) {
        this.connectorEventsSyncedAt = connectorEventsSyncedAt;
    }

    public Integer getConnectorSyncAttempts() {
        return connectorSyncAttempts;
    }

    public void setConnectorSyncAttempts(Integer connectorSyncAttempts) {
        this.connectorSyncAttempts = connectorSyncAttempts;
    }

    public String getUnidadeJudiciariaCodigo() {
        return unidadeJudiciariaCodigo;
    }

    public void setUnidadeJudiciariaCodigo(String unidadeJudiciariaCodigo) {
        this.unidadeJudiciariaCodigo = unidadeJudiciariaCodigo;
    }

    public Long getCatalogVersionId() {
        return catalogVersionId;
    }

    public void setCatalogVersionId(Long catalogVersionId) {
        this.catalogVersionId = catalogVersionId;
    }

    private static String limitText(String value, int maxLength) {
        if (value == null || maxLength < 1) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

}
