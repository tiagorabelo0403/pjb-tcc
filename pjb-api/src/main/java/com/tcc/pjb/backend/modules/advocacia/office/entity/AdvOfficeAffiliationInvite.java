package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeAffiliationInviteStatus;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeWorkspaceMode;
import com.tcc.pjb.backend.modules.advocacia.office.util.RamoDireitoSetConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "adv_office_affiliation_invite", indexes = {
        @Index(name = "ix_adv_office_affiliation_invite_equipe", columnList = "equipe_id"),
        @Index(name = "ix_adv_office_affiliation_invite_status", columnList = "status"),
        @Index(name = "ix_adv_office_affiliation_invite_target_user", columnList = "target_user_id")
})
@Getter
@Setter
public class AdvOfficeAffiliationInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_affiliation_invite_equipe"))
    private Equipe equipe;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "invited_nome", length = 150)
    private String invitedNome;

    @Column(name = "invited_email", length = 200)
    private String invitedEmail;

    @Column(name = "invited_cpf", length = 11)
    private String invitedCpf;

    @Column(name = "invited_oab", length = 64)
    private String invitedOab;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel_equipe", nullable = false, length = 40)
    private PapelEquipe papelEquipe;

    @Column(name = "cargo", length = 120)
    private String cargo;

    @Column(name = "allow_all_ramos", nullable = false)
    private boolean allowAllRamos;

    @Convert(converter = RamoDireitoSetConverter.class)
    @Column(name = "allowed_ramos_override", nullable = false, length = 1200)
    private Set<RamoDireito> allowedRamosOverride = EnumSet.noneOf(RamoDireito.class);

    @Column(name = "min_trust_for_auto")
    private Integer minTrustForAuto;

    @Column(name = "max_auto_por_dia")
    private Integer maxAutoPorDia;

    @Column(name = "block_personal_cases", nullable = false)
    private boolean blockPersonalCases;

    @Column(name = "auto_activate_on_accept", nullable = false)
    private boolean autoActivateOnAccept;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_on_accept", nullable = false, length = 20)
    private OfficeWorkspaceMode modeOnAccept = OfficeWorkspaceMode.HYBRID;

    @Column(name = "workspace_priority", nullable = false)
    private Integer workspacePriority = 100;

    @Column(name = "requires_final_approval", nullable = false)
    private boolean requiresFinalApproval;

    @Column(name = "required_assurance_level", length = 16)
    private String requiredAssuranceLevel;

    @Column(name = "invite_payload_hash", length = 128)
    private String invitePayloadHash;

    @Column(name = "recipient_identity_hash", length = 128)
    private String recipientIdentityHash;

    @Column(name = "invite_version", nullable = false)
    private Integer inviteVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "accepted_mode", length = 20)
    private OfficeWorkspaceMode acceptedMode;

    @Column(name = "accepted_auto_activate_on_login")
    private Boolean acceptedAutoActivateOnLogin;

    @Column(name = "accepted_allow_personal_own_cases")
    private Boolean acceptedAllowPersonalOwnCases;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by_user_id")
    private Long acceptedByUserId;

    @Column(name = "final_approved_at")
    private Instant finalApprovedAt;

    @Column(name = "final_approved_by_user_id")
    private Long finalApprovedByUserId;

    @Column(name = "final_approval_deadline_at")
    private Instant finalApprovalDeadlineAt;

    @Column(name = "final_approval_justification", length = 2000)
    private String finalApprovalJustification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OfficeAffiliationInviteStatus status = OfficeAffiliationInviteStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response_by_user_id")
    private Long responseByUserId;
}
