package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_usuario_avatar")
public class UsuarioAvatar {

  @Id
  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 80)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "source", nullable = false, length = 20)
  private String source;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UsuarioAvatar() {
  }

  public UsuarioAvatar(Long usuarioId,
      String storageKey,
      String contentType,
      long sizeBytes,
      String sha256,
      String source,
      Instant updatedAt) {
    this.usuarioId = usuarioId;
    this.storageKey = storageKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
    this.source = source;
    this.updatedAt = updatedAt;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public String getSource() {
    return source;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(String storageKey, String contentType, long sizeBytes, String sha256, String source, Instant updatedAt) {
    this.storageKey = storageKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
    this.source = source;
    this.updatedAt = updatedAt;
  }
}
