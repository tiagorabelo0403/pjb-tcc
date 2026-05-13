package com.tcc.pjb.backend.model.entity.professional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_professional_access_grant_event")
public class ProfessionalInstitutionalAccessGrantEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prof_access_grant_event_grant"))
    private ProfessionalInstitutionalAccessGrant grant;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private ProfessionalGrantEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private ProfessionalGrantApprovalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private ProfessionalGrantApprovalStatus newStatus;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_name", nullable = false, length = 180)
    private String actorName;

    @Column(name = "actor_class", nullable = false, length = 40)
    private String actorClass;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ProfessionalInstitutionalAccessGrant getGrant() { return grant; }
    public void setGrant(ProfessionalInstitutionalAccessGrant grant) { this.grant = grant; }
    public ProfessionalGrantEventType getEventType() { return eventType; }
    public void setEventType(ProfessionalGrantEventType eventType) { this.eventType = eventType; }
    public ProfessionalGrantApprovalStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ProfessionalGrantApprovalStatus previousStatus) { this.previousStatus = previousStatus; }
    public ProfessionalGrantApprovalStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ProfessionalGrantApprovalStatus newStatus) { this.newStatus = newStatus; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorClass() { return actorClass; }
    public void setActorClass(String actorClass) { this.actorClass = actorClass; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
