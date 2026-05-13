package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
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
@Table(name = "tb_inst_delivery_job_snapshot",
        indexes = {
                @Index(name = "idx_inst_delivery_job_status_next", columnList = "status_codigo, next_attempt_at"),
                @Index(name = "idx_inst_delivery_job_proc", columnList = "processo_id, status_codigo"),
                @Index(name = "idx_inst_delivery_job_unidade_kind", columnList = "unidade_codigo, destinatario_kind_codigo, status_codigo")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_delivery_job_jobid", columnNames = "job_id")
        })
public class InstitutionalDeliveryJobSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "job_id", nullable = false, length = 160)
    private String jobId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo", nullable = false, length = 180)
    private String caixaCodigo;

    @Column(name = "destinatario_kind_codigo", nullable = false, length = 80)
    private String destinatarioKindCodigo;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "canal_corrente", nullable = false, length = 80)
    private String canalCorrente;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "terminal_at")
    private Instant terminalAt;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalDeliveryJobSnapshot() {
    }

    public InstitutionalDeliveryJobSnapshot(String jobId,
                                            String expedicaoUuid,
                                            Long processoId,
                                            String unidadeCodigo,
                                            String caixaCodigo,
                                            String destinatarioKindCodigo,
                                            String statusCodigo,
                                            String canalCorrente,
                                            int attemptCount,
                                            Instant nextAttemptAt,
                                            Instant terminalAt,
                                            String snapshotHash,
                                            String snapshotJson,
                                            Instant createdAt,
                                            Instant updatedAt) {
        this.jobId = require(jobId, "jobId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = processoId;
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigo = require(caixaCodigo, "caixaCodigo");
        this.destinatarioKindCodigo = require(destinatarioKindCodigo, "destinatarioKindCodigo");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.canalCorrente = require(canalCorrente, "canalCorrente");
        this.attemptCount = attemptCount;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        this.terminalAt = terminalAt;
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getCaixaCodigo() { return caixaCodigo; }
    public String getDestinatarioKindCodigo() { return destinatarioKindCodigo; }
    public String getStatusCodigo() { return statusCodigo; }
    public String getCanalCorrente() { return canalCorrente; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getTerminalAt() { return terminalAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String statusCodigo, String canalCorrente, int attemptCount, Instant nextAttemptAt,
                        Instant terminalAt, String snapshotHash, String snapshotJson, Instant updatedAt) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.canalCorrente = require(canalCorrente, "canalCorrente");
        this.attemptCount = attemptCount;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        this.terminalAt = terminalAt;
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
