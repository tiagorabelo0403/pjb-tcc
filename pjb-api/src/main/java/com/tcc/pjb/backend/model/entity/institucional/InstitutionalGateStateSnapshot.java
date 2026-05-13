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
@Table(name = "tb_inst_gate_state_snapshot",
        indexes = {
                @Index(name = "idx_inst_gate_state_proc", columnList = "processo_id, status_codigo"),
                @Index(name = "idx_inst_gate_state_gate", columnList = "gate_code, status_codigo")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_gate_state_expedicao", columnNames = "expedicao_uuid")
        })
public class InstitutionalGateStateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "gate_state_id", nullable = false, length = 160)
    private String gateStateId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "gate_code", nullable = false, length = 160)
    private String gateCode;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "bloqueado", nullable = false)
    private boolean bloqueado;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalGateStateSnapshot() {
    }

    public InstitutionalGateStateSnapshot(String gateStateId,
                                          String expedicaoUuid,
                                          Long processoId,
                                          String gateCode,
                                          String statusCodigo,
                                          boolean bloqueado,
                                          Instant releasedAt,
                                          String snapshotHash,
                                          String snapshotJson,
                                          Instant createdAt,
                                          Instant updatedAt) {
        this.gateStateId = require(gateStateId, "gateStateId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = Objects.requireNonNull(processoId, "processoId");
        this.gateCode = require(gateCode, "gateCode");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.bloqueado = bloqueado;
        this.releasedAt = releasedAt;
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public String getGateStateId() { return gateStateId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getGateCode() { return gateCode; }
    public String getStatusCodigo() { return statusCodigo; }
    public boolean isBloqueado() { return bloqueado; }
    public Instant getReleasedAt() { return releasedAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String statusCodigo, boolean bloqueado, Instant releasedAt,
                        String snapshotHash, String snapshotJson, Instant updatedAt) {
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.bloqueado = bloqueado;
        this.releasedAt = releasedAt;
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
