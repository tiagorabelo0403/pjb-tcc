package com.tcc.pjb.backend.model.entity.professional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_professional_access_grant", indexes = {
        @Index(name = "idx_prof_access_grant_user_active", columnList = "usuario_id,ativo,inicio_vigencia,fim_vigencia"),
        @Index(name = "idx_prof_access_grant_actor_type", columnList = "actor_class,grant_type"),
        @Index(name = "idx_prof_access_grant_processo", columnList = "processo_id"),
        @Index(name = "idx_prof_access_grant_territory", columnList = "uf,comarca,tribunal,unidade_judiciaria_codigo")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalInstitutionalAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prof_access_grant_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", foreignKey = @ForeignKey(name = "fk_prof_access_grant_processo"))
    private Processo processo;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_class", nullable = false, length = 40)
    private ProfessionalActorClass actorClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false, length = 40)
    private ProfessionalAccessGrantType grantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_basis", nullable = false, length = 60)
    private ProfessionalAccessBasis accessBasis;

    @Column(name = "uf", length = 5)
    private String uf;

    @Column(name = "comarca", length = 160)
    private String comarca;

    @Column(name = "tribunal", length = 80)
    private String tribunal;

    @Column(name = "unidade_judiciaria_codigo", length = 80)
    private String unidadeJudiciariaCodigo;

    @Column(name = "orgao_colegiado_codigo", length = 80)
    private String orgaoColegiadoCodigo;

    @Column(name = "ente_code", length = 80)
    private String enteCode;

    @Column(name = "target_magistrate_user_id")
    private Long targetMagistrateUserId;

    @Column(name = "source_ref", length = 120)
    private String sourceRef;

    @Column(name = "source_label", length = 240)
    private String sourceLabel;

    @Column(name = "reason", length = 800)
    private String reason;

    @Column(name = "requires_step_up")
    private Boolean requiresStepUp;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ProfessionalGrantApprovalStatus approvalStatus = ProfessionalGrantApprovalStatus.PENDING;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "requested_by_name", length = 180)
    private String requestedByName;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_by_name", length = 180)
    private String approvedByName;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "decision_reason", length = 800)
    private String decisionReason;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "revoked_by_name", length = 180)
    private String revokedByName;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = Boolean.TRUE;

    @Column(name = "inicio_vigencia")
    private LocalDateTime inicioVigencia;

    @Column(name = "fim_vigencia")
    private LocalDateTime fimVigencia;

    public boolean requiresStepUp() {
        return Boolean.TRUE.equals(requiresStepUp);
    }

    public boolean isApproved() {
        return approvalStatus == ProfessionalGrantApprovalStatus.APPROVED;
    }

    public boolean isPending() {
        return approvalStatus == ProfessionalGrantApprovalStatus.PENDING;
    }

    public boolean isAtivoNaJanela(LocalDateTime reference) {
        if (!Boolean.TRUE.equals(ativo) || reference == null || approvalStatus != ProfessionalGrantApprovalStatus.APPROVED) {
            return false;
        }
        boolean started = inicioVigencia == null || !inicioVigencia.isAfter(reference);
        boolean notFinished = fimVigencia == null || !fimVigencia.isBefore(reference);
        return started && notFinished;
    }
}
