package com.tcc.pjb.backend.model.entity.calendar;

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
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_user_calendar_preference", indexes = {
        @Index(name = "idx_ucp_user", columnList = "usuario_id", unique = true)
})
public class UserCalendarPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "visible_lane_codes", length = 500)
    private String visibleLaneCodesRaw;

    @Column(name = "pinned_lane_codes", length = 500)
    private String pinnedLaneCodesRaw;

    @Column(name = "hidden_lane_codes", length = 500)
    private String hiddenLaneCodesRaw;

    @Column(name = "default_view", length = 20)
    private String defaultView;

    @Column(name = "include_personal_calendar", nullable = false)
    private boolean includePersonalCalendar;

    @Column(name = "include_institutional_calendar", nullable = false)
    private boolean includeInstitutionalCalendar;

    @Column(name = "highlight_urgent_days", nullable = false)
    private boolean highlightUrgentDays;

    @Column(name = "selected_scope_code", length = 64)
    private String selectedScopeCode;

    @Column(name = "selected_team_id")
    private Long selectedTeamId;

    @Column(name = "selected_institution_context_code", length = 64)
    private String selectedInstitutionContextCode;

    @Column(name = "notification_cadence_mode", length = 20)
    private String notificationCadenceMode;

    @Column(name = "notification_lane_codes", length = 500)
    private String notificationLaneCodesRaw;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getVisibleLaneCodesRaw() {
        return visibleLaneCodesRaw;
    }

    public void setVisibleLaneCodesRaw(String visibleLaneCodesRaw) {
        this.visibleLaneCodesRaw = visibleLaneCodesRaw;
    }

    public String getPinnedLaneCodesRaw() {
        return pinnedLaneCodesRaw;
    }

    public void setPinnedLaneCodesRaw(String pinnedLaneCodesRaw) {
        this.pinnedLaneCodesRaw = pinnedLaneCodesRaw;
    }

    public String getHiddenLaneCodesRaw() {
        return hiddenLaneCodesRaw;
    }

    public void setHiddenLaneCodesRaw(String hiddenLaneCodesRaw) {
        this.hiddenLaneCodesRaw = hiddenLaneCodesRaw;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public boolean isIncludePersonalCalendar() {
        return includePersonalCalendar;
    }

    public void setIncludePersonalCalendar(boolean includePersonalCalendar) {
        this.includePersonalCalendar = includePersonalCalendar;
    }

    public boolean isIncludeInstitutionalCalendar() {
        return includeInstitutionalCalendar;
    }

    public void setIncludeInstitutionalCalendar(boolean includeInstitutionalCalendar) {
        this.includeInstitutionalCalendar = includeInstitutionalCalendar;
    }

    public boolean isHighlightUrgentDays() {
        return highlightUrgentDays;
    }

    public void setHighlightUrgentDays(boolean highlightUrgentDays) {
        this.highlightUrgentDays = highlightUrgentDays;
    }

    public String getSelectedScopeCode() {
        return selectedScopeCode;
    }

    public void setSelectedScopeCode(String selectedScopeCode) {
        this.selectedScopeCode = selectedScopeCode;
    }

    public Long getSelectedTeamId() {
        return selectedTeamId;
    }

    public void setSelectedTeamId(Long selectedTeamId) {
        this.selectedTeamId = selectedTeamId;
    }

    public String getSelectedInstitutionContextCode() {
        return selectedInstitutionContextCode;
    }

    public void setSelectedInstitutionContextCode(String selectedInstitutionContextCode) {
        this.selectedInstitutionContextCode = selectedInstitutionContextCode;
    }

    public String getNotificationCadenceMode() {
        return notificationCadenceMode;
    }

    public void setNotificationCadenceMode(String notificationCadenceMode) {
        this.notificationCadenceMode = notificationCadenceMode;
    }

    public String getNotificationLaneCodesRaw() {
        return notificationLaneCodesRaw;
    }

    public void setNotificationLaneCodesRaw(String notificationLaneCodesRaw) {
        this.notificationLaneCodesRaw = notificationLaneCodesRaw;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
