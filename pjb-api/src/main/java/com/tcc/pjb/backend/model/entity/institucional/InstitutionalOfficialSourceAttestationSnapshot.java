package com.tcc.pjb.backend.model.entity.institucional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inst_official_source_attestation_snapshot",
        indexes = {
                @Index(name = "idx_inst_off_source_att_subject", columnList = "subject_type, subject_id"),
                @Index(name = "idx_inst_off_source_att_aff", columnList = "affiliation_id"),
                @Index(name = "idx_inst_off_source_att_req", columnList = "request_id"),
                @Index(name = "idx_inst_off_source_att_due", columnList = "next_refresh_at, due_now")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inst_off_source_att_subject", columnNames = {"subject_type", "subject_id"})
        })
public class InstitutionalOfficialSourceAttestationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 180)
    private String subjectId;

    @Column(name = "affiliation_id", length = 180)
    private String affiliationId;

    @Column(name = "request_id", length = 180)
    private String requestId;

    @Column(name = "organization_scope", length = 80)
    private String organizationScope;

    @Column(name = "orgao_sigla", length = 80)
    private String orgaoSigla;

    @Column(name = "unidade_codigo", length = 180)
    private String unidadeCodigo;

    @Column(name = "public_recognition_status", nullable = false, length = 80)
    private String publicRecognitionStatus;

    @Column(name = "attestation_status", nullable = false, length = 80)
    private String attestationStatus;

    @Column(name = "sovereign_recognition_ready", nullable = false)
    private boolean sovereignRecognitionReady;

    @Column(name = "due_now", nullable = false)
    private boolean dueNow;

    @Column(name = "automatic_refresh_eligible", nullable = false)
    private boolean automaticRefreshEligible;

    @Column(name = "last_attested_at", nullable = false)
    private Instant lastAttestedAt;

    @Column(name = "next_refresh_at")
    private Instant nextRefreshAt;

    @Column(name = "integrity_hash", nullable = false, length = 128)
    private String integrityHash;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionalOfficialSourceAttestationSnapshot() {
    }

    public InstitutionalOfficialSourceAttestationSnapshot(String subjectType,
                                                          String subjectId,
                                                          String affiliationId,
                                                          String requestId,
                                                          String organizationScope,
                                                          String orgaoSigla,
                                                          String unidadeCodigo,
                                                          String publicRecognitionStatus,
                                                          String attestationStatus,
                                                          boolean sovereignRecognitionReady,
                                                          boolean dueNow,
                                                          boolean automaticRefreshEligible,
                                                          Instant lastAttestedAt,
                                                          Instant nextRefreshAt,
                                                          String integrityHash,
                                                          String snapshotJson,
                                                          Instant createdAt,
                                                          Instant updatedAt) {
        this.subjectType = require(subjectType, "subjectType");
        this.subjectId = require(subjectId, "subjectId");
        this.affiliationId = normalize(affiliationId);
        this.requestId = normalize(requestId);
        this.organizationScope = normalize(organizationScope);
        this.orgaoSigla = normalize(orgaoSigla);
        this.unidadeCodigo = normalize(unidadeCodigo);
        this.publicRecognitionStatus = require(publicRecognitionStatus, "publicRecognitionStatus");
        this.attestationStatus = require(attestationStatus, "attestationStatus");
        this.sovereignRecognitionReady = sovereignRecognitionReady;
        this.dueNow = dueNow;
        this.automaticRefreshEligible = automaticRefreshEligible;
        this.lastAttestedAt = Objects.requireNonNull(lastAttestedAt, "lastAttestedAt");
        this.nextRefreshAt = nextRefreshAt;
        this.integrityHash = require(integrityHash, "integrityHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getAffiliationId() {
        return affiliationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void refresh(String affiliationId,
                        String requestId,
                        String organizationScope,
                        String orgaoSigla,
                        String unidadeCodigo,
                        String publicRecognitionStatus,
                        String attestationStatus,
                        boolean sovereignRecognitionReady,
                        boolean dueNow,
                        boolean automaticRefreshEligible,
                        Instant lastAttestedAt,
                        Instant nextRefreshAt,
                        String integrityHash,
                        String snapshotJson,
                        Instant updatedAt) {
        this.affiliationId = normalize(affiliationId);
        this.requestId = normalize(requestId);
        this.organizationScope = normalize(organizationScope);
        this.orgaoSigla = normalize(orgaoSigla);
        this.unidadeCodigo = normalize(unidadeCodigo);
        this.publicRecognitionStatus = require(publicRecognitionStatus, "publicRecognitionStatus");
        this.attestationStatus = require(attestationStatus, "attestationStatus");
        this.sovereignRecognitionReady = sovereignRecognitionReady;
        this.dueNow = dueNow;
        this.automaticRefreshEligible = automaticRefreshEligible;
        this.lastAttestedAt = Objects.requireNonNull(lastAttestedAt, "lastAttestedAt");
        this.nextRefreshAt = nextRefreshAt;
        this.integrityHash = require(integrityHash, "integrityHash");
        this.snapshotJson = require(snapshotJson, "snapshotJson");
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
