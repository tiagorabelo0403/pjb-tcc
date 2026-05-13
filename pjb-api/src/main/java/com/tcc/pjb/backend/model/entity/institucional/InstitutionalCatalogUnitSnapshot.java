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
@Table(name = "tb_inst_catalog_unit_snapshot",
        indexes = {
                @Index(name = "idx_inst_catalog_unit_kind_uf", columnList = "destinatario_kind, uf"),
                @Index(name = "idx_inst_catalog_unit_comarca", columnList = "uf, comarca, foro")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_catalog_unit_codigo_vigencia", columnNames = {"codigo_unidade", "vigencia_inicio"})
        })
public class InstitutionalCatalogUnitSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "codigo_unidade", nullable = false, length = 180)
    private String codigoUnidade;

    @Column(name = "destinatario_kind", nullable = false, length = 80)
    private String destinatarioKind;

    @Column(name = "uf", length = 8)
    private String uf;

    @Column(name = "comarca", length = 160)
    private String comarca;

    @Column(name = "foro", length = 160)
    private String foro;

    @Column(name = "ramo_direito", length = 80)
    private String ramoDireito;

    @Column(name = "grau_jurisdicao", length = 80)
    private String grauJurisdicao;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "vigencia_inicio", nullable = false)
    private Instant vigenciaInicio;

    @Column(name = "vigencia_fim")
    private Instant vigenciaFim;

    @Column(name = "snapshot_hash", nullable = false, length = 128)
    private String snapshotHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalCatalogUnitSnapshot() {
    }

    public InstitutionalCatalogUnitSnapshot(String codigoUnidade,
                                            String destinatarioKind,
                                            String uf,
                                            String comarca,
                                            String foro,
                                            String ramoDireito,
                                            String grauJurisdicao,
                                            boolean ativa,
                                            Instant vigenciaInicio,
                                            Instant vigenciaFim,
                                            String snapshotHash,
                                            String snapshotJson,
                                            Instant createdAt,
                                            Instant updatedAt) {
        this.codigoUnidade = require(codigoUnidade, "codigoUnidade");
        this.destinatarioKind = require(destinatarioKind, "destinatarioKind");
        this.uf = normalize(uf);
        this.comarca = normalize(comarca);
        this.foro = normalize(foro);
        this.ramoDireito = normalize(ramoDireito);
        this.grauJurisdicao = normalize(grauJurisdicao);
        this.ativa = ativa;
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        this.vigenciaFim = vigenciaFim;
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public String getCodigoUnidade() { return codigoUnidade; }
    public String getDestinatarioKind() { return destinatarioKind; }
    public String getUf() { return uf; }
    public String getComarca() { return comarca; }
    public String getForo() { return foro; }
    public String getRamoDireito() { return ramoDireito; }
    public String getGrauJurisdicao() { return grauJurisdicao; }
    public boolean isAtiva() { return ativa; }
    public Instant getVigenciaInicio() { return vigenciaInicio; }
    public Instant getVigenciaFim() { return vigenciaFim; }
    public String getSnapshotHash() { return snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(String snapshotHash, String snapshotJson, boolean ativa, Instant updatedAt) {
        this.snapshotHash = require(snapshotHash, "snapshotHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.ativa = ativa;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

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
