package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_pjb_subst_com_sync_cursor",
        indexes = {
                @Index(name = "ix_pjb_subst_com_cursor_exec", columnList = "execucao_id, janela_inicio"),
                @Index(name = "ix_pjb_subst_com_cursor_status", columnList = "tribunal_codigo, canal_origem, situacao, updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pjb_subst_com_cursor_exec_window", columnNames = {"execucao_id", "canal_origem", "janela_inicio", "janela_fim"})
        })
public class PjbSubstituicaoComunicacaoSyncCursorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execucao_id", nullable = false)
    private PjbSubstituicaoNacionalExecucaoEntity execucao;

    @Column(name = "tribunal_codigo", nullable = false, length = 24)
    private String tribunalCodigo;

    @Column(name = "canal_origem", nullable = false, length = 48)
    private String canalOrigem;

    @Column(name = "janela_inicio", nullable = false)
    private Instant janelaInicio;

    @Column(name = "janela_fim", nullable = false)
    private Instant janelaFim;

    @Column(name = "correlation_namespace", nullable = false, length = 120)
    private String correlationNamespace;

    @Column(name = "dedupe_namespace", nullable = false, length = 120)
    private String dedupeNamespace;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 32)
    private PjbSubstituicaoComunicacaoSyncSituacao situacao;

    @Column(name = "total_recebido", nullable = false)
    private int totalRecebido;

    @Column(name = "total_deduplicado", nullable = false)
    private int totalDeduplicado;

    @Column(name = "total_correlacionado", nullable = false)
    private int totalCorrelacionado;

    @Column(name = "total_reprocessavel", nullable = false)
    private int totalReprocessavel;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PjbSubstituicaoComunicacaoSyncCursorEntity() {
    }

    public PjbSubstituicaoComunicacaoSyncCursorEntity(PjbSubstituicaoNacionalExecucaoEntity execucao,
                                                      String tribunalCodigo,
                                                      String canalOrigem,
                                                      Instant janelaInicio,
                                                      Instant janelaFim,
                                                      String correlationNamespace,
                                                      String dedupeNamespace,
                                                      PjbSubstituicaoComunicacaoSyncSituacao situacao,
                                                      int totalRecebido,
                                                      int totalDeduplicado,
                                                      int totalCorrelacionado,
                                                      int totalReprocessavel,
                                                      String snapshotJson,
                                                      Instant createdAt,
                                                      Instant updatedAt) {
        this.execucao = Objects.requireNonNull(execucao, "execucao");
        this.tribunalCodigo = require(tribunalCodigo, "tribunalCodigo");
        this.canalOrigem = require(canalOrigem, "canalOrigem");
        this.janelaInicio = Objects.requireNonNull(janelaInicio, "janelaInicio");
        this.janelaFim = Objects.requireNonNull(janelaFim, "janelaFim");
        this.correlationNamespace = require(correlationNamespace, "correlationNamespace");
        this.dedupeNamespace = require(dedupeNamespace, "dedupeNamespace");
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.totalRecebido = totalRecebido;
        this.totalDeduplicado = totalDeduplicado;
        this.totalCorrelacionado = totalCorrelacionado;
        this.totalReprocessavel = totalReprocessavel;
        this.snapshotJson = requireJson(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public PjbSubstituicaoNacionalExecucaoEntity getExecucao() { return execucao; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public String getCanalOrigem() { return canalOrigem; }
    public Instant getJanelaInicio() { return janelaInicio; }
    public Instant getJanelaFim() { return janelaFim; }
    public String getCorrelationNamespace() { return correlationNamespace; }
    public String getDedupeNamespace() { return dedupeNamespace; }
    public PjbSubstituicaoComunicacaoSyncSituacao getSituacao() { return situacao; }
    public int getTotalRecebido() { return totalRecebido; }
    public int getTotalDeduplicado() { return totalDeduplicado; }
    public int getTotalCorrelacionado() { return totalCorrelacionado; }
    public int getTotalReprocessavel() { return totalReprocessavel; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(PjbSubstituicaoComunicacaoSyncSituacao situacao,
                        int totalRecebido,
                        int totalDeduplicado,
                        int totalCorrelacionado,
                        int totalReprocessavel,
                        String snapshotJson,
                        Instant updatedAt) {
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.totalRecebido = totalRecebido;
        this.totalDeduplicado = totalDeduplicado;
        this.totalCorrelacionado = totalCorrelacionado;
        this.totalReprocessavel = totalReprocessavel;
        this.snapshotJson = requireJson(snapshotJson, "snapshotJson");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String requireJson(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value;
    }
}
