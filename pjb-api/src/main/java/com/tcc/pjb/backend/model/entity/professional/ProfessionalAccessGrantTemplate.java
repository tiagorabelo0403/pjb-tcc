package com.tcc.pjb.backend.model.entity.professional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_professional_access_grant_template", indexes = {
        @Index(name = "idx_prof_access_grant_template_code", columnList = "template_code", unique = true),
        @Index(name = "idx_prof_access_grant_template_actor", columnList = "actor_class,grant_type,ativo")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalAccessGrantTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "label", nullable = false, length = 180)
    private String label;

    @Column(name = "description", length = 800)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_class", nullable = false, length = 40)
    private ProfessionalActorClass actorClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false, length = 40)
    private ProfessionalAccessGrantType grantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_basis", nullable = false, length = 60)
    private ProfessionalAccessBasis accessBasis;

    @Column(name = "default_requires_step_up", nullable = false)
    private Boolean defaultRequiresStepUp;

    @Column(name = "auto_approve_allowed", nullable = false)
    private Boolean autoApproveAllowed;

    @Column(name = "default_duration_days")
    private Integer defaultDurationDays;

    @Column(name = "target_mode", nullable = false, length = 40)
    private String targetMode;

    @Column(name = "default_uf", length = 5)
    private String defaultUf;

    @Column(name = "default_comarca", length = 160)
    private String defaultComarca;

    @Column(name = "default_tribunal", length = 80)
    private String defaultTribunal;

    @Column(name = "default_unidade_judiciaria_codigo", length = 80)
    private String defaultUnidadeJudiciariaCodigo;

    @Column(name = "default_orgao_colegiado_codigo", length = 80)
    private String defaultOrgaoColegiadoCodigo;

    @Column(name = "default_ente_code", length = 80)
    private String defaultEnteCode;

    @Column(name = "governance_tone", length = 40)
    private String governanceTone;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = Boolean.TRUE;
}
