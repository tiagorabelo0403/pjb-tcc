package com.tcc.pjb.backend.repository.upload;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.upload.UploadBatch;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, UUID> {
    List<UploadBatch> findTop20ByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
