package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_operational_coverage_rule_snapshot",
        indexes = {
                @Index(name = "idx_inst_cov_rule_unidade", columnList = "unidade_codigo, updated_at"),
                @Index(name = "idx_inst_cov_rule_caixa", columnList = "caixa_codigo, updated_at"),
                @Index(name = "idx_inst_cov_rule_status", columnList = "status_codigo, updated_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_inst_cov_rule_rule_id", columnNames = "rule_id"))
public class InstitutionalOperationalCoverageRuleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "rule_id", nullable = false, length = 160)
    private String ruleId;

    @Column(name = "unidade_codigo", nullable = false, length = 160)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo", nullable = false, length = 160)
    private String caixaCodigo;

    @Column(name = "titular_usuario_id", nullable = false)
    private Long titularUsuarioId;

    @Column(name = "cobertura_usuario_id", nullable = false)
    private Long coberturaUsuarioId;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "tipo_cobertura", nullable = false, length = 80)
    private String tipoCobertura;

    @Column(name = "hash_integridade", nullable = false, length = 128)
    private String hashIntegridade;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalOperationalCoverageRuleSnapshot() {
    }

    public InstitutionalOperationalCoverageRuleSnapshot(String ruleId,
                                                        String unidadeCodigo,
                                                        String caixaCodigo,
                                                        Long titularUsuarioId,
                                                        Long coberturaUsuarioId,
                                                        String statusCodigo,
                                                        String tipoCobertura,
                                                        String hashIntegridade,
                                                        String snapshotJson,
                                                        Instant createdAt,
                                                        Instant updatedAt) {
        this.ruleId = require(ruleId, "ruleId");
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigo = require(caixaCodigo, "caixaCodigo");
        this.titularUsuarioId = titularUsuarioId;
        this.coberturaUsuarioId = coberturaUsuarioId;
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.tipoCobertura = require(tipoCobertura, "tipoCobertura");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getRuleId() { return ruleId; }
    public String getSnapshotJson() { return snapshotJson; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getCaixaCodigo() { return caixaCodigo; }
    public String getStatusCodigo() { return statusCodigo; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String statusCodigo, String hashIntegridade, String snapshotJson, Instant updatedAt) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.updatedAt = updatedAt;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
