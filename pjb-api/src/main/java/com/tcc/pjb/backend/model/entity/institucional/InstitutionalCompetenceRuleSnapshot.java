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
@Table(name = "tb_inst_competence_rule_snapshot",
        indexes = {
                @Index(name = "idx_inst_comp_rule_kind_papel", columnList = "destinatario_kind, papel_processual, prioridade"),
                @Index(name = "idx_inst_comp_rule_local", columnList = "uf, comarca, foro")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_comp_rule_id", columnNames = {"rule_id"})
        })
public class InstitutionalCompetenceRuleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "rule_id", nullable = false, length = 180)
    private String ruleId;

    @Column(name = "destinatario_kind", nullable = false, length = 80)
    private String destinatarioKind;

    @Column(name = "papel_processual", nullable = false, length = 80)
    private String papelProcessual;

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

    @Column(name = "unidade_codigo", nullable = false, length = 180)
    private String unidadeCodigo;

    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

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

    protected InstitutionalCompetenceRuleSnapshot() {
    }

    public InstitutionalCompetenceRuleSnapshot(String ruleId,
                                               String destinatarioKind,
                                               String papelProcessual,
                                               String uf,
                                               String comarca,
                                               String foro,
                                               String ramoDireito,
                                               String grauJurisdicao,
                                               String unidadeCodigo,
                                               int prioridade,
                                               boolean ativa,
                                               Instant vigenciaInicio,
                                               Instant vigenciaFim,
                                               String snapshotJson,
                                               Instant createdAt,
                                               Instant updatedAt) {
        this.ruleId = require(ruleId, "ruleId");
        this.destinatarioKind = require(destinatarioKind, "destinatarioKind");
        this.papelProcessual = require(papelProcessual, "papelProcessual");
        this.uf = normalize(uf);
        this.comarca = normalize(comarca);
        this.foro = normalize(foro);
        this.ramoDireito = normalize(ramoDireito);
        this.grauJurisdicao = normalize(grauJurisdicao);
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.prioridade = prioridade;
        this.ativa = ativa;
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        this.vigenciaFim = vigenciaFim;
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public String getRuleId() { return ruleId; }
    public String getSnapshotJson() { return snapshotJson; }

    public void refresh(String uf,
                        String comarca,
                        String foro,
                        String ramoDireito,
                        String grauJurisdicao,
                        String unidadeCodigo,
                        int prioridade,
                        boolean ativa,
                        Instant vigenciaInicio,
                        Instant vigenciaFim,
                        Instant updatedAt,
                        String snapshotJson) {
        this.uf = normalize(uf);
        this.comarca = normalize(comarca);
        this.foro = normalize(foro);
        this.ramoDireito = normalize(ramoDireito);
        this.grauJurisdicao = normalize(grauJurisdicao);
        this.unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        this.prioridade = prioridade;
        this.ativa = ativa;
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
