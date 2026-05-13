package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoMigracaoLoteSituacao;
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
@Table(name = "tb_pjb_subst_migracao_lote",
        indexes = {
                @Index(name = "ix_pjb_subst_mig_lote_exec", columnList = "execucao_id, lote_ordem"),
                @Index(name = "ix_pjb_subst_mig_lote_status", columnList = "tribunal_codigo, situacao, updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pjb_subst_mig_lote_exec_codigo", columnNames = {"execucao_id", "lote_codigo"})
        })
public class PjbSubstituicaoMigracaoLoteEntity {

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

    @Column(name = "lote_codigo", nullable = false, length = 64)
    private String loteCodigo;

    @Column(name = "lote_ordem", nullable = false)
    private int loteOrdem;

    @Column(name = "faixa_referencia", nullable = false, length = 160)
    private String faixaReferencia;

    @Column(name = "total_itens", nullable = false)
    private int totalItens;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 32)
    private PjbSubstituicaoMigracaoLoteSituacao situacao;

    @Column(name = "checksum_esperado", nullable = false, length = 128)
    private String checksumEsperado;

    @Column(name = "checksum_apurado", length = 128)
    private String checksumApurado;

    @Column(name = "divergencias", nullable = false)
    private int divergencias;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PjbSubstituicaoMigracaoLoteEntity() {
    }

    public PjbSubstituicaoMigracaoLoteEntity(PjbSubstituicaoNacionalExecucaoEntity execucao,
                                             String tribunalCodigo,
                                             String loteCodigo,
                                             int loteOrdem,
                                             String faixaReferencia,
                                             int totalItens,
                                             PjbSubstituicaoMigracaoLoteSituacao situacao,
                                             String checksumEsperado,
                                             String checksumApurado,
                                             int divergencias,
                                             String snapshotJson,
                                             Instant createdAt,
                                             Instant updatedAt) {
        this.execucao = Objects.requireNonNull(execucao, "execucao");
        this.tribunalCodigo = require(tribunalCodigo, "tribunalCodigo");
        this.loteCodigo = require(loteCodigo, "loteCodigo");
        this.loteOrdem = loteOrdem;
        this.faixaReferencia = require(faixaReferencia, "faixaReferencia");
        this.totalItens = totalItens;
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.checksumEsperado = require(checksumEsperado, "checksumEsperado");
        this.checksumApurado = normalize(checksumApurado);
        this.divergencias = divergencias;
        this.snapshotJson = requireJson(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public PjbSubstituicaoNacionalExecucaoEntity getExecucao() { return execucao; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public String getLoteCodigo() { return loteCodigo; }
    public int getLoteOrdem() { return loteOrdem; }
    public String getFaixaReferencia() { return faixaReferencia; }
    public int getTotalItens() { return totalItens; }
    public PjbSubstituicaoMigracaoLoteSituacao getSituacao() { return situacao; }
    public String getChecksumEsperado() { return checksumEsperado; }
    public String getChecksumApurado() { return checksumApurado; }
    public int getDivergencias() { return divergencias; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(PjbSubstituicaoMigracaoLoteSituacao situacao,
                        String checksumApurado,
                        int divergencias,
                        String snapshotJson,
                        Instant updatedAt) {
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.checksumApurado = normalize(checksumApurado);
        this.divergencias = divergencias;
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

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
