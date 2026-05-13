package com.tcc.pjb.backend.model.entity.upload;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_upload_item")
public class UploadItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private UploadBatch batch;

    @Column(name = "nome_original", length = 255, nullable = false)
    private String nomeOriginal;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "hash_sha384", length = 96)
    private String hashSha384;

    @Column(name = "storage_backend", length = 40, nullable = false)
    private String storageBackend;

    @Column(name = "storage_key", length = 600, nullable = false)
    private String storageKey;

    @Column(name = "storage_uri", length = 900, nullable = false)
    private String storageUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private UploadItemStatus status;

    @Column(name = "edge_attestation_json", length = 12000)
    private String edgeAttestationJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    @Column(name = "linked_document_id")
    private UUID linkedDocumentId;
}
