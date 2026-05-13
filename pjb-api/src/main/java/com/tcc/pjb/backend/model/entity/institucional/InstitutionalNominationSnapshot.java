package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_nomination_snapshot",
        indexes = {
                @Index(name = "idx_inst_nomination_user", columnList = "nominated_user_id, updated_at"),
                @Index(name = "idx_inst_nomination_unidade", columnList = "unidade_codigo, updated_at"),
                @Index(name = "idx_inst_nomination_status", columnList = "status_codigo, updated_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_inst_nomination_id", columnNames = "nomination_id"))
public class InstitutionalNominationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "nomination_id", nullable = false, length = 160)
    private String nominationId;

    @Column(name = "affiliation_id", nullable = false, length = 160)
    private String affiliationId;

    @Column(name = "nominated_user_id", nullable = false)
    private Long nominatedUserId;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo", nullable = false, length = 180)
    private String caixaCodigo;

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

    protected InstitutionalNominationSnapshot() {
    }

    public InstitutionalNominationSnapshot(String nominationId,
                                           String affiliationId,
                                           Long nominatedUserId,
                                           String unidadeCodigo,
                                           String caixaCodigo,
                                           String statusCodigo,
                                           String hashIntegridade,
                                           String snapshotJson,
                                           Instant createdAt,
                                           Instant updatedAt) {
        this.nominationId = require(nominationId, "nominationId");
        this.affiliationId = require(affiliationId, "affiliationId");
        this.nominatedUserId = nominatedUserId;
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigo = require(caixaCodigo, "caixaCodigo");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getNominationId() { return nominationId; }
    public String getSnapshotJson() { return snapshotJson; }
    public Long getNominatedUserId() { return nominatedUserId; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getCaixaCodigo() { return caixaCodigo; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String statusCodigo, String hashIntegridade, String snapshotJson, Instant updatedAt) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.updatedAt = updatedAt;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " é obrigatório");
        return value.trim();
    }
}
