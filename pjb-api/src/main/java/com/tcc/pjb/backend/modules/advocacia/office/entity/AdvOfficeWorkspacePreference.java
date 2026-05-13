package com.tcc.pjb.backend.modules.advocacia.office.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "adv_office_workspace_preference", indexes = {
        @Index(name = "idx_adv_office_workspace_user", columnList = "usuario_id", unique = true)
})
public class AdvOfficeWorkspacePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "preferred_equipe_id")
    private Long preferredEquipeId;

    @Column(name = "mode", nullable = false, length = 16)
    private String mode;

    @Column(name = "auto_activate_on_login", nullable = false)
    private boolean autoActivateOnLogin;

    @Column(name = "allow_personal_own_cases", nullable = false)
    private boolean allowPersonalOwnCases;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getPreferredEquipeId() { return preferredEquipeId; }
    public void setPreferredEquipeId(Long preferredEquipeId) { this.preferredEquipeId = preferredEquipeId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isAutoActivateOnLogin() { return autoActivateOnLogin; }
    public void setAutoActivateOnLogin(boolean autoActivateOnLogin) { this.autoActivateOnLogin = autoActivateOnLogin; }
    public boolean isAllowPersonalOwnCases() { return allowPersonalOwnCases; }
    public void setAllowPersonalOwnCases(boolean allowPersonalOwnCases) { this.allowPersonalOwnCases = allowPersonalOwnCases; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
