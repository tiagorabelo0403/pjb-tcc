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
@Table(name = "tb_inst_delivery_attempt_snapshot",
        indexes = {
                @Index(name = "idx_inst_delivery_attempt_job", columnList = "job_id, attempt_number"),
                @Index(name = "idx_inst_delivery_attempt_expedicao", columnList = "expedicao_uuid, ended_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_delivery_attempt_id", columnNames = "attempt_id")
        })
public class InstitutionalDeliveryAttemptSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "attempt_id", nullable = false, length = 160)
    private String attemptId;

    @Column(name = "job_id", nullable = false, length = 160)
    private String jobId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "channel_code", nullable = false, length = 80)
    private String channelCode;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InstitutionalDeliveryAttemptSnapshot() {
    }

    public InstitutionalDeliveryAttemptSnapshot(String attemptId,
                                                String jobId,
                                                String expedicaoUuid,
                                                int attemptNumber,
                                                String statusCodigo,
                                                String channelCode,
                                                Instant endedAt,
                                                String snapshotHash,
                                                String snapshotJson,
                                                Instant createdAt) {
        this.attemptId = require(attemptId, "attemptId");
        this.jobId = require(jobId, "jobId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.attemptNumber = attemptNumber;
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.channelCode = require(channelCode, "channelCode");
        this.endedAt = Objects.requireNonNull(endedAt, "endedAt");
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Long getId() { return id; }
    public String getAttemptId() { return attemptId; }
    public String getJobId() { return jobId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getStatusCodigo() { return statusCodigo; }
    public String getChannelCode() { return channelCode; }
    public Instant getEndedAt() { return endedAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
