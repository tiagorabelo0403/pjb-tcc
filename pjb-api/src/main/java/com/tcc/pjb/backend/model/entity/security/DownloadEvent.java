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
import org.hibernate.annotations.CreationTimestamp;
import com.tcc.pjb.backend.model.entity.Usuario;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "download_events",
        indexes = {
                @Index(name = "idx_de_user_time", columnList = "usuario_id, created_at"),
                @Index(name = "idx_de_device_time", columnList = "device_id, created_at"),
                @Index(name = "idx_de_user_processo_time", columnList = "usuario_id, processo_id, created_at"),
                @Index(name = "idx_de_user_documento_time", columnList = "usuario_id, documento_id, created_at")
        }
)
public class DownloadEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_de_user"))
    private Usuario usuario;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "documento_id", length = 36)
    private String documentoId;

    @Column(name = "path", nullable = false, length = 300)
    private String path;

    @Column(name = "bytes", nullable = false)
    private long bytes;

    @Column(name = "watermark_id", length = 96)
    private String watermarkId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public Long getProcessoId() { return processoId; }
    public void setProcessoId(Long processoId) { this.processoId = processoId; }

    public String getDocumentoId() { return documentoId; }
    public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getBytes() { return bytes; }
    public void setBytes(long bytes) { this.bytes = bytes; }

    public String getWatermarkId() { return watermarkId; }
    public void setWatermarkId(String watermarkId) { this.watermarkId = watermarkId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadEvent)) return false;
        DownloadEvent that = (DownloadEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
