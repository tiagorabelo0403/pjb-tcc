package com.tcc.pjb.backend.repository.upload;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.upload.UploadItem;
import com.tcc.pjb.backend.model.entity.upload.UploadItemStatus;

public interface UploadItemRepository extends JpaRepository<UploadItem, UUID> {
    List<UploadItem> findByBatchIdOrderByCreatedAtAsc(UUID batchId);
    List<UploadItem> findByBatchIdAndStatusOrderByCreatedAtAsc(UUID batchId, UploadItemStatus status);
    Page<UploadItem> findByBatchIdAndStatus(UUID batchId, UploadItemStatus status, Pageable pageable);
    long countByBatch_Id(UUID batchId);
    boolean existsByBatch_IdAndHashSha384(UUID batchId, String hashSha384);

    @Query("select coalesce(sum(i.tamanhoBytes), 0) from UploadItem i where i.batch.id = :batchId")
    long sumTamanhoBytesByBatchId(@Param("batchId") UUID batchId);
}
