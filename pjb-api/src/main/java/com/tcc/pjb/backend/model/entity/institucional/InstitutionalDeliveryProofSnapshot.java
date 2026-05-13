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
@Table(name = "tb_inst_delivery_proof_snapshot",
        indexes = {
                @Index(name = "idx_inst_delivery_proof_expedicao", columnList = "expedicao_uuid, created_at"),
                @Index(name = "idx_inst_delivery_proof_proc", columnList = "processo_id, etapa")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_delivery_proof_proofid", columnNames = "proof_id")
        })
public class InstitutionalDeliveryProofSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "proof_id", nullable = false, length = 160)
    private String proofId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "etapa", nullable = false, length = 120)
    private String etapa;

    @Column(name = "canal", nullable = false, length = 80)
    private String canal;

    @Column(name = "evidencia_tipo", nullable = false, length = 120)
    private String evidenciaTipo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    protected InstitutionalDeliveryProofSnapshot() {
    }

    public InstitutionalDeliveryProofSnapshot(String proofId,
                                              String expedicaoUuid,
                                              Long processoId,
                                              String etapa,
                                              String canal,
                                              String evidenciaTipo,
                                              Instant createdAt,
                                              String snapshotHash,
                                              String snapshotJson) {
        this.proofId = require(proofId, "proofId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = Objects.requireNonNull(processoId, "processoId");
        this.etapa = require(etapa, "etapa");
        this.canal = require(canal, "canal");
        this.evidenciaTipo = require(evidenciaTipo, "evidenciaTipo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
    }

    public Long getId() { return id; }
    public String getProofId() { return proofId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getEtapa() { return etapa; }
    public String getCanal() { return canal; }
    public String getEvidenciaTipo() { return evidenciaTipo; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
