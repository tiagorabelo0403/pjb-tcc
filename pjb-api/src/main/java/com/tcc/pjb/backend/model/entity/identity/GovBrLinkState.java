package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_govbr_link_state")
public class GovBrLinkState {

  @Id
  @Column(name = "state_id", nullable = false)
  private UUID stateId;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "cpf", nullable = false, length = 11)
  private String cpf;

  @Column(name = "code_verifier", nullable = false, length = 128)
  private String codeVerifier;

  @Column(name = "nonce", nullable = false, length = 96)
  private String nonce;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected GovBrLinkState() {
  }

  public GovBrLinkState(UUID stateId, Long usuarioId, String cpf, String codeVerifier, String nonce, Instant expiresAt, Instant createdAt) {
    this.stateId = stateId;
    this.usuarioId = usuarioId;
    this.cpf = cpf;
    this.codeVerifier = codeVerifier;
    this.nonce = nonce;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public UUID getStateId() {
    return stateId;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getCpf() {
    return cpf;
  }

  public String getCodeVerifier() {
    return codeVerifier;
  }

  public String getNonce() {
    return nonce;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed(Instant now) {
    this.usedAt = now;
  }
}
