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
@Table(name = "tb_inst_dead_letter_snapshot",
        indexes = {
                @Index(name = "idx_inst_dead_letter_proc", columnList = "processo_id, created_at"),
                @Index(name = "idx_inst_dead_letter_expedicao", columnList = "expedicao_uuid, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_dead_letter_entry", columnNames = "entry_id")
        })
public class InstitutionalDeadLetterSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "entry_id", nullable = false, length = 160)
    private String entryId;

    @Column(name = "job_id", nullable = false, length = 160)
    private String jobId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @Column(name = "channel_code", nullable = false, length = 80)
    private String channelCode;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InstitutionalDeadLetterSnapshot() {
    }

    public InstitutionalDeadLetterSnapshot(String entryId,
                                           String jobId,
                                           String expedicaoUuid,
                                           Long processoId,
                                           String reasonCode,
                                           String channelCode,
                                           String snapshotHash,
                                           String snapshotJson,
                                           Instant createdAt) {
        this.entryId = require(entryId, "entryId");
        this.jobId = require(jobId, "jobId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = processoId;
        this.reasonCode = normalize(reasonCode);
        this.channelCode = require(channelCode, "channelCode");
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Long getId() { return id; }
    public String getEntryId() { return entryId; }
    public String getJobId() { return jobId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getReasonCode() { return reasonCode; }
    public String getChannelCode() { return channelCode; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
