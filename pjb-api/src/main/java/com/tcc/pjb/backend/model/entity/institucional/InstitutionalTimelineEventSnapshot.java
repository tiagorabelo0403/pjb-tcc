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
@Table(name = "tb_inst_timeline_event_snapshot",
        indexes = {
                @Index(name = "idx_inst_timeline_event_expedicao", columnList = "expedicao_uuid, occurred_at"),
                @Index(name = "idx_inst_timeline_event_proc", columnList = "processo_id, occurred_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_timeline_event_eventid", columnNames = "event_id")
        })
public class InstitutionalTimelineEventSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "event_id", nullable = false, length = 160)
    private String eventId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo", nullable = false, length = 180)
    private String caixaCodigo;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    protected InstitutionalTimelineEventSnapshot() {
    }

    public InstitutionalTimelineEventSnapshot(String eventId,
                                              String expedicaoUuid,
                                              Long processoId,
                                              String eventType,
                                              String statusCodigo,
                                              String unidadeCodigo,
                                              String caixaCodigo,
                                              Instant occurredAt,
                                              String snapshotHash,
                                              String snapshotJson) {
        this.eventId = require(eventId, "eventId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = Objects.requireNonNull(processoId, "processoId");
        this.eventType = require(eventType, "eventType");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigo = require(caixaCodigo, "caixaCodigo");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getEventType() { return eventType; }
    public String getStatusCodigo() { return statusCodigo; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getCaixaCodigo() { return caixaCodigo; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
