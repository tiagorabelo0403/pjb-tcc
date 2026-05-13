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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Usuario;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "trusted_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_device_credential", columnNames = {"credential_id"}),
                @UniqueConstraint(name = "uk_user_device_alias", columnNames = {"usuario_id", "alias"})
        },
        indexes = {
                @Index(name = "idx_device_user", columnList = "usuario_id"),
                @Index(name = "idx_device_active", columnList = "usuario_id, revogado_em"),
                @Index(name = "idx_device_pending", columnList = "pending_challenge_id")
        }
)
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_usuario"))
    private Usuario usuario;

    @NotBlank
    @Size(max = 512)
    @Column(name = "credential_id", nullable = false, length = 512)
    private String credentialId;

    @NotBlank
    @Size(max = 4096)
    @Column(name = "public_key", nullable = false, length = 4096)
    private String publicKey;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String alias;

    @Size(max = 64)
    @Column(length = 64)
    private String aaguid;

    @Size(max = 40)
    @Column(name = "attestation_fmt", length = 40)
    private String attestationFmt;

    @Column(name = "attestation_trusted", nullable = false)
    private boolean attestationTrusted;

    @Column(name = "sign_count", nullable = false)
    private long signCount = 0L;

    @Size(max = 64)
    @Column(name = "enroll_ip", length = 64)
    private String enrollIp;

    @Size(max = 120)
    @Column(name = "enroll_network", length = 120)
    private String enrollNetwork;

    @Column(name = "enroll_suspect_network", nullable = false)
    private boolean enrollSuspectNetwork;

    @Column(name = "risk_score_enroll", nullable = false)
    private int riskScoreEnroll;

    @Column(name = "quarentena_ate")
    private LocalDateTime quarentenaAte;

    @Column(name = "pending_challenge_id")
    private Long pendingChallengeId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "ultimo_uso_em")
    private LocalDateTime ultimoUsoEm;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    public boolean isRevogado() { return revogadoEm != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getAaguid() { return aaguid; }
    public void setAaguid(String aaguid) { this.aaguid = aaguid; }

    public String getAttestationFmt() { return attestationFmt; }
    public void setAttestationFmt(String attestationFmt) { this.attestationFmt = attestationFmt; }

    public boolean isAttestationTrusted() { return attestationTrusted; }
    public Boolean getAttestationTrusted() { return attestationTrusted; }
    public void setAttestationTrusted(boolean attestationTrusted) { this.attestationTrusted = attestationTrusted; }

    public long getSignCount() { return signCount; }
    public void setSignCount(long signCount) { this.signCount = signCount; }

    public String getEnrollIp() { return enrollIp; }
    public void setEnrollIp(String enrollIp) { this.enrollIp = enrollIp; }

    public String getEnrollNetwork() { return enrollNetwork; }
    public void setEnrollNetwork(String enrollNetwork) { this.enrollNetwork = enrollNetwork; }

    public boolean isEnrollSuspectNetwork() { return enrollSuspectNetwork; }
    public void setEnrollSuspectNetwork(boolean enrollSuspectNetwork) { this.enrollSuspectNetwork = enrollSuspectNetwork; }

    public int getRiskScoreEnroll() { return riskScoreEnroll; }
    public void setRiskScoreEnroll(int riskScoreEnroll) { this.riskScoreEnroll = riskScoreEnroll; }

    public LocalDateTime getQuarentenaAte() { return quarentenaAte; }
    public void setQuarentenaAte(LocalDateTime quarentenaAte) { this.quarentenaAte = quarentenaAte; }

    public Long getPendingChallengeId() { return pendingChallengeId; }
    public void setPendingChallengeId(Long pendingChallengeId) { this.pendingChallengeId = pendingChallengeId; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public LocalDateTime getUltimoUsoEm() { return ultimoUsoEm; }
    public void setUltimoUsoEm(LocalDateTime ultimoUsoEm) { this.ultimoUsoEm = ultimoUsoEm; }

    public LocalDateTime getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(LocalDateTime revogadoEm) { this.revogadoEm = revogadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrustedDevice)) return false;
        TrustedDevice that = (TrustedDevice) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
