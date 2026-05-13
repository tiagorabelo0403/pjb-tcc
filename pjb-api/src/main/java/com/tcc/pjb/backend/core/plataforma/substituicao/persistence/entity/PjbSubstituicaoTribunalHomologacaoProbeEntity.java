package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoHomologacaoProbeSituacao;
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
@Table(name = "tb_pjb_subst_homologacao_probe",
        indexes = {
                @Index(name = "ix_pjb_subst_hom_probe_exec", columnList = "execucao_id, probe_codigo"),
                @Index(name = "ix_pjb_subst_hom_probe_status", columnList = "tribunal_codigo, situacao, updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pjb_subst_hom_probe_exec_probe", columnNames = {"execucao_id", "probe_codigo"})
        })
public class PjbSubstituicaoTribunalHomologacaoProbeEntity {

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

    @Column(name = "probe_codigo", nullable = false, length = 80)
    private String probeCodigo;

    @Column(name = "connector_codigo", nullable = false, length = 40)
    private String connectorCodigo;

    @Column(name = "ambiente_codigo", nullable = false, length = 24)
    private String ambienteCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 24)
    private PjbSubstituicaoHomologacaoProbeSituacao situacao;

    @Column(name = "gate_score", nullable = false)
    private int gateScore;

    @Column(name = "evidencias_json", nullable = false, columnDefinition = "text")
    private String evidenciasJson;

    @Column(name = "resultado_json", nullable = false, columnDefinition = "text")
    private String resultadoJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PjbSubstituicaoTribunalHomologacaoProbeEntity() {
    }

    public PjbSubstituicaoTribunalHomologacaoProbeEntity(PjbSubstituicaoNacionalExecucaoEntity execucao,
                                                         String tribunalCodigo,
                                                         String probeCodigo,
                                                         String connectorCodigo,
                                                         String ambienteCodigo,
                                                         PjbSubstituicaoHomologacaoProbeSituacao situacao,
                                                         int gateScore,
                                                         String evidenciasJson,
                                                         String resultadoJson,
                                                         Instant createdAt,
                                                         Instant updatedAt) {
        this.execucao = Objects.requireNonNull(execucao, "execucao");
        this.tribunalCodigo = require(tribunalCodigo, "tribunalCodigo");
        this.probeCodigo = require(probeCodigo, "probeCodigo");
        this.connectorCodigo = require(connectorCodigo, "connectorCodigo");
        this.ambienteCodigo = require(ambienteCodigo, "ambienteCodigo");
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.gateScore = gateScore;
        this.evidenciasJson = requireJson(evidenciasJson, "evidenciasJson");
        this.resultadoJson = requireJson(resultadoJson, "resultadoJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public PjbSubstituicaoNacionalExecucaoEntity getExecucao() { return execucao; }
    public String getTribunalCodigo() { return tribunalCodigo; }
    public String getProbeCodigo() { return probeCodigo; }
    public String getConnectorCodigo() { return connectorCodigo; }
    public String getAmbienteCodigo() { return ambienteCodigo; }
    public PjbSubstituicaoHomologacaoProbeSituacao getSituacao() { return situacao; }
    public int getGateScore() { return gateScore; }
    public String getEvidenciasJson() { return evidenciasJson; }
    public String getResultadoJson() { return resultadoJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(PjbSubstituicaoHomologacaoProbeSituacao situacao,
                        int gateScore,
                        String evidenciasJson,
                        String resultadoJson,
                        Instant updatedAt) {
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.gateScore = gateScore;
        this.evidenciasJson = requireJson(evidenciasJson, "evidenciasJson");
        this.resultadoJson = requireJson(resultadoJson, "resultadoJson");
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
