package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_institutional_policy_profile")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionalPolicyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "equipe_id")
    private Long equipeId;

    @Column(name = "policy_key", length = 160, nullable = false)
    private String policyKey;

    @Column(name = "policy_tier", length = 80, nullable = false)
    private String policyTier;

    @Column(name = "policy_version", length = 80, nullable = false)
    private String policyVersion;

    @Column(name = "ramo_direito", length = 80)
    private String ramoDireito;

    @Column(name = "materia", length = 80)
    private String materia;

    @Column(name = "rito_processual", length = 120)
    private String ritoProcessual;

    @Column(name = "classe_processual", length = 160)
    private String classeProcessual;

    @Column(name = "fase_processual", length = 80)
    private String faseProcessual;

    @Column(name = "tribunal_codigo", length = 80)
    private String tribunalCodigo;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "strict_release", nullable = false)
    private boolean strictRelease;

    @Lob
    @Column(name = "mandatory_directives", columnDefinition = "TEXT")
    private String mandatoryDirectives;

    @Lob
    @Column(name = "blocking_directives", columnDefinition = "TEXT")
    private String blockingDirectives;

    @Lob
    @Column(name = "release_guardrails", columnDefinition = "TEXT")
    private String releaseGuardrails;

    @Lob
    @Column(name = "escalation_triggers", columnDefinition = "TEXT")
    private String escalationTriggers;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (dataCriacao == null) {
            dataCriacao = now;
        }
        dataAtualizacao = now;
        if (policyVersion == null || policyVersion.isBlank()) {
            policyVersion = "POLICY/2026.1";
        }
        if (policyTier == null || policyTier.isBlank()) {
            policyTier = "PROCESSO";
        }
        if (policyKey == null || policyKey.isBlank()) {
            policyKey = "DEFAULT_POLICY";
        }
        ramoDireito = normalizeAxis(ramoDireito);
        materia = normalizeAxis(materia);
        ritoProcessual = normalizeAxis(ritoProcessual);
        faseProcessual = normalizeAxis(faseProcessual);
        tribunalCodigo = normalizeAxis(tribunalCodigo);
        classeProcessual = normalizeFreeText(classeProcessual);
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
        ramoDireito = normalizeAxis(ramoDireito);
        materia = normalizeAxis(materia);
        ritoProcessual = normalizeAxis(ritoProcessual);
        faseProcessual = normalizeAxis(faseProcessual);
        tribunalCodigo = normalizeAxis(tribunalCodigo);
        classeProcessual = normalizeFreeText(classeProcessual);
    }

    private String normalizeAxis(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeFreeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}

