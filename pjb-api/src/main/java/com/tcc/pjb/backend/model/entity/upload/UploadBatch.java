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
@Table(name = "tb_upload_batch")
public class UploadBatch {

    @Id
    private UUID id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "expected_count")
    private Integer expectedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private UploadBatchStatus status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;
}
