package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_external_dispatch_snapshot",
        indexes = {
                @Index(name = "idx_inst_external_dispatch_exp", columnList = "expedicao_uuid, updated_at"),
                @Index(name = "idx_inst_external_dispatch_proc", columnList = "processo_id, updated_at"),
                @Index(name = "idx_inst_external_dispatch_status", columnList = "status_codigo, updated_at"),
                @Index(name = "idx_inst_external_dispatch_unidade_kind", columnList = "unidade_codigo, destinatario_kind_codigo, status_codigo")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_external_dispatch_id", columnNames = "dispatch_id")
        })
public class InstitutionalExternalDispatchSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "dispatch_id", nullable = false, length = 160)
    private String dispatchId;

    @Column(name = "job_id", nullable = false, length = 160)
    private String jobId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "destinatario_kind_codigo", nullable = false, length = 80)
    private String destinatarioKindCodigo;

    @Column(name = "canal_codigo", nullable = false, length = 80)
    private String canalCodigo;

    @Column(name = "provider_codigo", nullable = false, length = 80)
    private String providerCodigo;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalExternalDispatchSnapshot() {
    }

    public InstitutionalExternalDispatchSnapshot(String dispatchId,
                                                String jobId,
                                                String expedicaoUuid,
                                                Long processoId,
                                                String unidadeCodigo,
                                                String destinatarioKindCodigo,
                                                String canalCodigo,
                                                String providerCodigo,
                                                String statusCodigo,
                                                String providerReference,
                                                String failureReason,
                                                String payloadHash,
                                                String snapshotJson,
                                                Instant createdAt,
                                                Instant updatedAt) {
        this.dispatchId = require(dispatchId, "dispatchId");
        this.jobId = require(jobId, "jobId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = processoId;
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.destinatarioKindCodigo = require(destinatarioKindCodigo, "destinatarioKindCodigo");
        this.canalCodigo = require(canalCodigo, "canalCodigo");
        this.providerCodigo = require(providerCodigo, "providerCodigo");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.providerReference = providerReference;
        this.failureReason = failureReason;
        this.payloadHash = require(payloadHash, "payloadHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDispatchId() { return dispatchId; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getProcessoId() { return processoId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public String getStatusCodigo() { return statusCodigo; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getDestinatarioKindCodigo() { return destinatarioKindCodigo; }

    public void refresh(String statusCodigo, String providerReference, String failureReason, String responsePayload, Instant updatedAt, String payloadHash, String snapshotJson) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.providerReference = providerReference;
        this.failureReason = failureReason != null ? failureReason + (responsePayload == null || responsePayload.isBlank() ? "" : " | response=" + responsePayload) : null;
        this.updatedAt = updatedAt;
        this.payloadHash = require(payloadHash, "payloadHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
