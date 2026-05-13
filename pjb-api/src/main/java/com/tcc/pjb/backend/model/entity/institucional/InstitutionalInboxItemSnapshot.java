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
@Table(name = "tb_inst_inbox_item_snapshot",
        indexes = {
                @Index(name = "idx_inst_inbox_item_proc", columnList = "processo_id, status_codigo"),
                @Index(name = "idx_inst_inbox_item_unidade_caixa", columnList = "unidade_codigo, caixa_codigo_atual, status_codigo")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_inbox_item_expedicao", columnNames = "expedicao_uuid")
        })
public class InstitutionalInboxItemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "inbox_item_id", nullable = false, length = 160)
    private String inboxItemId;

    @Column(name = "expedicao_uuid", nullable = false, length = 160)
    private String expedicaoUuid;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "caixa_codigo_atual", nullable = false, length = 180)
    private String caixaCodigoAtual;

    @Column(name = "status_codigo", nullable = false, length = 80)
    private String statusCodigo;

    @Column(name = "prazo_ciencia_em")
    private Instant prazoCienciaEm;

    @Column(name = "prazo_resposta_em")
    private Instant prazoRespostaEm;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalInboxItemSnapshot() {
    }

    public InstitutionalInboxItemSnapshot(String inboxItemId,
                                          String expedicaoUuid,
                                          Long processoId,
                                          String unidadeCodigo,
                                          String caixaCodigoAtual,
                                          String statusCodigo,
                                          Instant prazoCienciaEm,
                                          Instant prazoRespostaEm,
                                          String snapshotHash,
                                          String snapshotJson,
                                          Instant createdAt,
                                          Instant updatedAt) {
        this.inboxItemId = require(inboxItemId, "inboxItemId");
        this.expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        this.processoId = Objects.requireNonNull(processoId, "processoId");
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.caixaCodigoAtual = require(caixaCodigoAtual, "caixaCodigoAtual");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.prazoCienciaEm = prazoCienciaEm;
        this.prazoRespostaEm = prazoRespostaEm;
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public String getInboxItemId() { return inboxItemId; }
    public String getExpedicaoUuid() { return expedicaoUuid; }
    public Long getProcessoId() { return processoId; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getCaixaCodigoAtual() { return caixaCodigoAtual; }
    public String getStatusCodigo() { return statusCodigo; }
    public Instant getPrazoCienciaEm() { return prazoCienciaEm; }
    public Instant getPrazoRespostaEm() { return prazoRespostaEm; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String caixaCodigoAtual, String statusCodigo, Instant prazoCienciaEm, Instant prazoRespostaEm,
                        String snapshotHash, String snapshotJson, Instant updatedAt) {
        this.caixaCodigoAtual = require(caixaCodigoAtual, "caixaCodigoAtual");
        this.statusCodigo = require(statusCodigo, "statusCodigo");
        this.prazoCienciaEm = prazoCienciaEm;
        this.prazoRespostaEm = prazoRespostaEm;
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
