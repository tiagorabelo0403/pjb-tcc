package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;




import java.time.OffsetDateTime;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoRuleProposalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_rito_rule_proposal")
public class RitoRuleProposal {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "rito_resolved", nullable = false, length = 64)
    private String ritoResolved;

    @Column(name = "rito_chosen", nullable = false, length = 64)
    private String ritoChosen;

    @Column(name = "occurrences", nullable = false)
    private Integer occurrences;

    @Column(name = "sample_reasons_json", length = 12000)
    private String sampleReasonsJson;

    



    @Column(name = "requires_dual_approval", nullable = false)
    private boolean requiresDualApproval;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RitoRuleProposalStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "decision_notes")
    private String decisionNotes;

    @Column(name = "first_reviewed_at")
    private OffsetDateTime firstReviewedAt;

    @Column(name = "first_reviewed_by_user_id")
    private Long firstReviewedByUserId;

    @Column(name = "first_decision_notes")
    private String firstDecisionNotes;

    @Column(name = "second_decision_notes")
    private String secondDecisionNotes;
}
