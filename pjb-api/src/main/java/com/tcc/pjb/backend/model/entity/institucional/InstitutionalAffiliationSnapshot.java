package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_affiliation_snapshot",
        indexes = {
                @Index(name = "idx_inst_affiliation_kind", columnList = "destinatario_kind, updated_at"),
                @Index(name = "idx_inst_affiliation_unidade", columnList = "unidade_codigo, updated_at"),
                @Index(name = "idx_inst_affiliation_status", columnList = "status_codigo, updated_at"),
                @Index(name = "idx_inst_affiliation_scope", columnList = "organization_scope, updated_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_inst_affiliation_id", columnNames = "affiliation_id"))
public class InstitutionalAffiliationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "affiliation_id", nullable = false, length = 160)
    private String affiliationId;

    @Column(name = "destinatario_kind", nullable = false, length = 100)
    private String destinatarioKind;

    @Column(name = "organization_scope", length = 100)
    private String organizationScope;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "orgao_sigla", nullable = false, length = 120)
    private String orgaoSigla;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "hash_integridade", nullable = false, length = 128)
    private String hashIntegridade;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalAffiliationSnapshot() {
    }

    public InstitutionalAffiliationSnapshot(String affiliationId,
                                            String destinatarioKind,
                                            String organizationScope,
                                            String unidadeCodigo,
                                            String orgaoSigla,
                                            String statusCodigo,
                                            String hashIntegridade,
                                            String snapshotJson,
                                            Instant createdAt,
                                            Instant updatedAt) {
        this.affiliationId = require(affiliationId, "affiliationId");
        this.destinatarioKind = require(destinatarioKind, "destinatarioKind");
        this.organizationScope = normalizeNullable(organizationScope);
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.orgaoSigla = require(orgaoSigla, "orgaoSigla");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getAffiliationId() { return affiliationId; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getStatusCodigo() { return statusCodigo; }
    public String getUnidadeCodigo() { return unidadeCodigo; }

    public void refresh(String organizationScope, String statusCodigo, String hashIntegridade, String snapshotJson, Instant updatedAt) {
        this.organizationScope = normalizeNullable(organizationScope);
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.updatedAt = updatedAt;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " é obrigatório");
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
