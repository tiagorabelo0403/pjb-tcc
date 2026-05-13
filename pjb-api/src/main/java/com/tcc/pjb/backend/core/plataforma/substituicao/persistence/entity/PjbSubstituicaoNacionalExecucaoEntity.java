package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tb_pjb_substituicao_execucao", indexes = {
        @Index(name = "ix_pjb_subst_exec_tribunal", columnList = "tribunal_codigo,acao,situacao,created_at"),
        @Index(name = "ix_pjb_subst_exec_job", columnList = "job_id"),
        @Index(name = "ix_pjb_subst_exec_request_hash", columnList = "request_hash", unique = true)
})
public class PjbSubstituicaoNacionalExecucaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "tribunal_codigo", nullable = false, length = 24)
    private String tribunalCodigo;

    @Column(name = "tribunal_nome", nullable = false, length = 180)
    private String tribunalNome;

    @Column(name = "ramo_justica", nullable = false, length = 48)
    private String ramoJustica;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 64)
    private PjbSubstituicaoExecucaoAcao acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 32)
    private PjbSubstituicaoExecucaoSituacao situacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_atual", nullable = false, length = 32)
    private PjbSubstituicaoExecucaoFase faseAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_execucao", nullable = false, length = 24)
    private PjbSubstituicaoExecucaoModo modoExecucao;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Column(name = "gate_aprovado", nullable = false)
    private boolean gateAprovado;

    @Column(name = "rollback_reversivel", nullable = false)
    private boolean rollbackReversivel;

    @Column(name = "gate_score", nullable = false)
    private int gateScore;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "justificativa", length = 1000)
    private String justificativa;

    @Column(name = "onda_alvo", length = 64)
    private String ondaAlvo;

    @Lob
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Lob
    @Column(name = "resultado_json", columnDefinition = "text")
    private String resultadoJson;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PjbSubstituicaoNacionalExecucaoEntity() {
    }

    public PjbSubstituicaoNacionalExecucaoEntity(String tribunalCodigo,
                                                 String tribunalNome,
                                                 String ramoJustica,
                                                 PjbSubstituicaoExecucaoAcao acao,
                                                 PjbSubstituicaoExecucaoModo modoExecucao,
                                                 boolean dryRun,
                                                 String requestHash,
                                                 String requestedBy,
                                                 String justificativa,
                                                 String ondaAlvo,
                                                 String payloadJson) {
        this.tribunalCodigo = normalize(tribunalCodigo);
        this.tribunalNome = normalize(tribunalNome);
        this.ramoJustica = normalize(ramoJustica);
        this.acao = Objects.requireNonNull(acao);
        this.modoExecucao = Objects.requireNonNull(modoExecucao);
        this.dryRun = dryRun;
        this.requestHash = normalize(requestHash);
        this.requestedBy = normalize(requestedBy);
        this.justificativa = normalizeNullable(justificativa);
        this.ondaAlvo = normalizeNullable(ondaAlvo);
        this.payloadJson = payloadJson;
        this.situacao = PjbSubstituicaoExecucaoSituacao.RECEBIDA;
        this.faseAtual = PjbSubstituicaoExecucaoFase.RECEPCAO;
        this.gateAprovado = false;
        this.rollbackReversivel = false;
        this.gateScore = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public String getTribunalNome() { return tribunalNome; }
    public String getRamoJustica() { return ramoJustica; }
    public PjbSubstituicaoExecucaoAcao getAcao() { return acao; }
    public PjbSubstituicaoExecucaoSituacao getSituacao() { return situacao; }
    public PjbSubstituicaoExecucaoFase getFaseAtual() { return faseAtual; }
    public PjbSubstituicaoExecucaoModo getModoExecucao() { return modoExecucao; }
    public boolean isDryRun() { return dryRun; }
    public boolean isGateAprovado() { return gateAprovado; }
    public boolean isRollbackReversivel() { return rollbackReversivel; }
    public int getGateScore() { return gateScore; }
    public UUID getJobId() { return jobId; }
    public String getCorrelationId() { return correlationId; }
    public String getRequestHash() { return requestHash; }
    public String getRequestedBy() { return requestedBy; }
    public String getJustificativa() { return justificativa; }
    public String getOndaAlvo() { return ondaAlvo; }
    public String getPayloadJson() { return payloadJson; }
    public String getResultadoJson() { return resultadoJson; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void enfileirar(UUID jobId, String correlationId) {
        this.jobId = jobId;
        this.correlationId = normalizeNullable(correlationId);
        this.situacao = PjbSubstituicaoExecucaoSituacao.ENFILEIRADA;
        this.faseAtual = PjbSubstituicaoExecucaoFase.RECEPCAO;
        this.updatedAt = Instant.now();
    }

    public void iniciar(PjbSubstituicaoExecucaoFase faseAtual, int gateScore, boolean gateAprovado, boolean rollbackReversivel) {
        this.faseAtual = Objects.requireNonNull(faseAtual);
        this.gateScore = Math.max(0, Math.min(100, gateScore));
        this.gateAprovado = gateAprovado;
        this.rollbackReversivel = rollbackReversivel;
        this.situacao = PjbSubstituicaoExecucaoSituacao.EM_EXECUCAO;
        this.startedAt = this.startedAt == null ? Instant.now() : this.startedAt;
        this.updatedAt = Instant.now();
    }

    public void atualizarFase(PjbSubstituicaoExecucaoFase faseAtual, String resultadoJson) {
        this.faseAtual = Objects.requireNonNull(faseAtual);
        if (resultadoJson != null) {
            this.resultadoJson = resultadoJson;
        }
        this.updatedAt = Instant.now();
    }

    public void concluir(PjbSubstituicaoExecucaoFase faseAtual, String resultadoJson, boolean gateAprovado, boolean rollbackReversivel, int gateScore) {
        this.faseAtual = Objects.requireNonNull(faseAtual);
        this.resultadoJson = resultadoJson;
        this.gateAprovado = gateAprovado;
        this.rollbackReversivel = rollbackReversivel;
        this.gateScore = Math.max(0, Math.min(100, gateScore));
        this.situacao = PjbSubstituicaoExecucaoSituacao.CONCLUIDA;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void bloquear(PjbSubstituicaoExecucaoFase faseAtual, String resultadoJson, boolean gateAprovado, boolean rollbackReversivel, int gateScore) {
        this.faseAtual = Objects.requireNonNull(faseAtual);
        this.resultadoJson = resultadoJson;
        this.gateAprovado = gateAprovado;
        this.rollbackReversivel = rollbackReversivel;
        this.gateScore = Math.max(0, Math.min(100, gateScore));
        this.situacao = PjbSubstituicaoExecucaoSituacao.BLOQUEADA;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void falhar(PjbSubstituicaoExecucaoFase faseAtual, String resultadoJson) {
        this.faseAtual = Objects.requireNonNull(faseAtual);
        this.resultadoJson = resultadoJson;
        this.situacao = PjbSubstituicaoExecucaoSituacao.FALHA;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void reencaminhar() {
        this.situacao = PjbSubstituicaoExecucaoSituacao.ENFILEIRADA;
        this.completedAt = null;
        this.updatedAt = Instant.now();
    }

    private static String normalize(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "" : normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
