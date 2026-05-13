package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_draft_manifestation_snapshot",
        indexes = {
                @Index(name = "idx_inst_draft_exp", columnList = "expedicao_uuid, updated_at"),
                @Index(name = "idx_inst_draft_proc", columnList = "processo_id, updated_at"),
                @Index(name = "idx_inst_draft_status", columnList = "status_codigo, updated_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_inst_draft_id", columnNames = "draft_id"))
public class InstitutionalDraftManifestationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "draft_id", nullable = false, length = 160)
    private String draftId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "unidade_codigo", nullable = false, length = 160)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo", nullable = false, length = 160)
    private String caixaCodigo;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "aprovador_usuario_id")
    private Long aprovadorUsuarioId;

    @Column(name = "hash_integridade", nullable = false, length = 128)
    private String hashIntegridade;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalDraftManifestationSnapshot() {
    }

    public InstitutionalDraftManifestationSnapshot(String draftId,
                                                   String expedicaoUuid,
                                                   Long processoId,
                                                   String unidadeCodigo,
                                                   String caixaCodigo,
                                                   String statusCodigo,
                                                   Long aprovadorUsuarioId,
                                                   String hashIntegridade,
                                                   String snapshotJson,
                                                   Instant createdAt,
                                                   Instant updatedAt) {
        this.draftId = require(draftId, "draftId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = processoId;
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigo = require(caixaCodigo, "caixaCodigo");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.aprovadorUsuarioId = aprovadorUsuarioId;
        this.hashIntegridade = require(hashIntegridade, "hashIntegridade");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDraftId() { return draftId; }
    public String getSnapshotJson() { return snapshotJson; }
    public Long getProcessoId() { return processoId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getStatusCodigo() { return statusCodigo; }

    public void refresh(String statusCodigo, Long aprovadorUsuarioId, String hashIntegridade, String snapshotJson, Instant updatedAt) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.aprovadorUsuarioId = aprovadorUsuarioId;
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
