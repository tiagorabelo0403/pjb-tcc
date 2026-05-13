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
@Table(name = "tb_inst_catalog_governance_snapshot",
        indexes = {
                @Index(name = "idx_inst_catalog_governance_unit", columnList = "unidade_codigo, vigencia_inicio"),
                @Index(name = "idx_inst_catalog_governance_kind_uf", columnList = "destinatario_kind, uf")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_catalog_governance_id", columnNames = {"governance_id"})
        })
public class InstitutionalCatalogGovernanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "governance_id", nullable = false, length = 180)
    private String governanceId;

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

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

    @Column(name = "abrangencia", nullable = false, length = 40)
    private String abrangencia;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "suspende_entrega_externa", nullable = false)
    private boolean suspendeEntregaExterna;

    @Column(name = "exige_homologacao_admin", nullable = false)
    private boolean exigeHomologacaoAdministrativa;

    @Column(name = "unidade_substituta_codigo", length = 180)
    private String unidadeSubstitutaCodigo;

    @Column(name = "vigencia_inicio", nullable = false)
    private Instant vigenciaInicio;

    @Column(name = "vigencia_fim")
    private Instant vigenciaFim;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalCatalogGovernanceSnapshot() {
    }

    public InstitutionalCatalogGovernanceSnapshot(String governanceId,
                                                  String unidadeCodigo,
                                                  String destinatarioKind,
                                                  String uf,
                                                  String comarca,
                                                  String foro,
                                                  String ramoDireito,
                                                  String grauJurisdicao,
                                                  String abrangencia,
                                                  boolean ativa,
                                                  boolean suspendeEntregaExterna,
                                                  boolean exigeHomologacaoAdministrativa,
                                                  String unidadeSubstitutaCodigo,
                                                  Instant vigenciaInicio,
                                                  Instant vigenciaFim,
                                                  String snapshotJson,
                                                  Instant createdAt,
                                                  Instant updatedAt) {
        this.governanceId = require(governanceId, "governanceId");
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.destinatarioKind = require(destinatarioKind, "destinatarioKind");
        this.uf = normalize(uf);
        this.comarca = normalize(comarca);
        this.foro = normalize(foro);
        this.ramoDireito = normalize(ramoDireito);
        this.grauJurisdicao = normalize(grauJurisdicao);
        this.abrangencia = require(abrangencia, "abrangencia");
        this.ativa = ativa;
        this.suspendeEntregaExterna = suspendeEntregaExterna;
        this.exigeHomologacaoAdministrativa = exigeHomologacaoAdministrativa;
        this.unidadeSubstitutaCodigo = normalize(unidadeSubstitutaCodigo);
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        this.vigenciaFim = vigenciaFim;
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public String getGovernanceId() { return governanceId; }
    public String getUnidadeCodigo() { return unidadeCodigo; }
    public String getSnapshotJson() { return snapshotJson; }

    public void refresh(String unidadeCodigo,
                        String destinatarioKind,
                        String uf,
                        String comarca,
                        String foro,
                        String ramoDireito,
                        String grauJurisdicao,
                        String abrangencia,
                        boolean ativa,
                        boolean suspendeEntregaExterna,
                        boolean exigeHomologacaoAdministrativa,
                        String unidadeSubstitutaCodigo,
                        Instant vigenciaInicio,
                        Instant vigenciaFim,
                        Instant updatedAt,
                        String snapshotJson) {
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.destinatarioKind = require(destinatarioKind, "destinatarioKind");
        this.uf = normalize(uf);
        this.comarca = normalize(comarca);
        this.foro = normalize(foro);
        this.ramoDireito = normalize(ramoDireito);
        this.grauJurisdicao = normalize(grauJurisdicao);
        this.abrangencia = require(abrangencia, "abrangencia");
        this.ativa = ativa;
        this.suspendeEntregaExterna = suspendeEntregaExterna;
        this.exigeHomologacaoAdministrativa = exigeHomologacaoAdministrativa;
        this.unidadeSubstitutaCodigo = normalize(unidadeSubstitutaCodigo);
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        this.vigenciaFim = vigenciaFim;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
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
