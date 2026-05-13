package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Usuario;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "user_security_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_security_profile", columnNames = {"usuario_id"})
        },
        indexes = {
                @Index(name = "idx_usp_user", columnList = "usuario_id")
        }
)
public class UserSecurityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usp_usuario"))
    private Usuario usuario;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    @Column(name = "last_login_network", length = 120)
    private String lastLoginNetwork;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_strong_auth_at")
    private LocalDateTime lastStrongAuthAt;


    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "frozen_until")
    private LocalDateTime frozenUntil;

    @Column(name = "freeze_reason", length = 200)
    private String freezeReason;

    @Column(name = "adv_baptized_at")
    private LocalDateTime advBaptizedAt;

    @Column(name = "adv_baptism_method", length = 40)
    private String advBaptismMethod;

    @Column(name = "adv_baptism_cert_fingerprint", length = 96)
    private String advBaptismCertFingerprint;

    @Column(name = "totp_secret", length = 120)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "gov_verified_at")
    private LocalDateTime govVerifiedAt;

    @Column(name = "gov_verified_sub_hash", length = 64)
    private String govVerifiedSubHash;

    @Column(name = "gov_email_hash", length = 64)
    private String govEmailHash;

    @Column(name = "gov_phone_hash", length = 64)
    private String govPhoneHash;

    @Column(name = "gov_email_verified", nullable = false)
    private boolean govEmailVerified = false;

    @Column(name = "gov_phone_verified", nullable = false)
    private boolean govPhoneVerified = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    public String getLastLoginNetwork() { return lastLoginNetwork; }
    public void setLastLoginNetwork(String lastLoginNetwork) { this.lastLoginNetwork = lastLoginNetwork; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getLastStrongAuthAt() { return lastStrongAuthAt; }
    public void setLastStrongAuthAt(LocalDateTime lastStrongAuthAt) { this.lastStrongAuthAt = lastStrongAuthAt; }


    public LocalDateTime getFrozenAt() { return frozenAt; }
    public void setFrozenAt(LocalDateTime frozenAt) { this.frozenAt = frozenAt; }

    public LocalDateTime getFrozenUntil() { return frozenUntil; }
    public void setFrozenUntil(LocalDateTime frozenUntil) { this.frozenUntil = frozenUntil; }

    public String getFreezeReason() { return freezeReason; }
    public void setFreezeReason(String freezeReason) { this.freezeReason = freezeReason; }

    public boolean isFrozen() {
        return isFrozenNow();
    }

    public boolean isFrozenNow() {
        if (frozenAt == null) return false;
        if (frozenUntil == null) return true;
        return frozenUntil.isAfter(LocalDateTime.now());
    }

    public LocalDateTime getAdvBaptizedAt() { return advBaptizedAt; }
    public void setAdvBaptizedAt(LocalDateTime advBaptizedAt) { this.advBaptizedAt = advBaptizedAt; }

    public String getAdvBaptismMethod() { return advBaptismMethod; }
    public void setAdvBaptismMethod(String advBaptismMethod) { this.advBaptismMethod = advBaptismMethod; }

    public String getAdvBaptismCertFingerprint() { return advBaptismCertFingerprint; }
    public void setAdvBaptismCertFingerprint(String advBaptismCertFingerprint) { this.advBaptismCertFingerprint = advBaptismCertFingerprint; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public LocalDateTime getGovVerifiedAt() { return govVerifiedAt; }
    public void setGovVerifiedAt(LocalDateTime govVerifiedAt) { this.govVerifiedAt = govVerifiedAt; }

    public String getGovVerifiedSubHash() { return govVerifiedSubHash; }
    public void setGovVerifiedSubHash(String govVerifiedSubHash) { this.govVerifiedSubHash = govVerifiedSubHash; }

    public String getGovEmailHash() { return govEmailHash; }
    public void setGovEmailHash(String govEmailHash) { this.govEmailHash = govEmailHash; }

    public String getGovPhoneHash() { return govPhoneHash; }
    public void setGovPhoneHash(String govPhoneHash) { this.govPhoneHash = govPhoneHash; }

    public boolean isGovEmailVerified() { return govEmailVerified; }
    public void setGovEmailVerified(boolean govEmailVerified) { this.govEmailVerified = govEmailVerified; }

    public boolean isGovPhoneVerified() { return govPhoneVerified; }
    public void setGovPhoneVerified(boolean govPhoneVerified) { this.govPhoneVerified = govPhoneVerified; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSecurityProfile)) return false;
        UserSecurityProfile that = (UserSecurityProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
