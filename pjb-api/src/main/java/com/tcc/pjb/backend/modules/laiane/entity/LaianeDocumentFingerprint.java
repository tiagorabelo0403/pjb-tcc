package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.*;

@Entity
@Table(name = "tb_laiane_document_fingerprint",
        uniqueConstraints = {@UniqueConstraint(name = "uk_laiane_doc_user_hash", columnNames = {"usuario_id", "sha256"})})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDocumentFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "sha256", length = 64, nullable = false)
    private String sha256;

    @Column(name = "mime", length = 120)
    private String mime;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
