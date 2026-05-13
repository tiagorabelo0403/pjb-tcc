package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.upload.UploadBatch;
import com.tcc.pjb.backend.model.entity.upload.UploadItem;
import com.tcc.pjb.backend.model.entity.upload.UploadItemStatus;
import com.tcc.pjb.backend.repository.upload.UploadBatchRepository;
import com.tcc.pjb.backend.repository.upload.UploadItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeDocumentBatchGovernanceService {

    private final UploadBatchRepository uploadBatchRepository;
    private final UploadItemRepository uploadItemRepository;

    public OfficeDocumentBatchGovernanceService(UploadBatchRepository uploadBatchRepository,
                                                UploadItemRepository uploadItemRepository) {
        this.uploadBatchRepository = Objects.requireNonNull(uploadBatchRepository);
        this.uploadItemRepository = Objects.requireNonNull(uploadItemRepository);
    }

    @Transactional(readOnly = true)
    public DocumentBatchSnapshot snapshot(UUID batchId) {
        UploadBatch batch = uploadBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Lote de upload nao encontrado."));
        List<UploadItem> items = uploadItemRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        long uploadedCount = items.stream().filter(item -> item.getStatus() == UploadItemStatus.UPLOADED).count();
        long reservedCount = items.stream().filter(item -> item.getStatus() == UploadItemStatus.RESERVED).count();
        long linkedCount = items.stream().filter(item -> item.getStatus() == UploadItemStatus.LINKED_TO_PROCESS).count();
        long failedCount = items.stream().filter(item -> item.getStatus() == UploadItemStatus.FAILED).count();
        long totalBytes = items.stream().map(UploadItem::getTamanhoBytes).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        String fingerprint = fingerprint(batch, items);
        return new DocumentBatchSnapshot(
                batch.getId(),
                batch.getProcessoId(),
                batch.getCreatedBy(),
                batch.getStatus().name(),
                batch.getExpectedCount(),
                items.size(),
                uploadedCount,
                reservedCount,
                linkedCount,
                failedCount,
                totalBytes,
                fingerprint
        );
    }

    public String fingerprint(UploadBatch batch, List<UploadItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(batch.getId()).append('|')
                .append(batch.getProcessoId()).append('|')
                .append(batch.getStatus().name()).append('|')
                .append(batch.getExpectedCount()).append('|');
        for (UploadItem item : items) {
            sb.append(item.getId()).append('|')
                    .append(item.getStatus()).append('|')
                    .append(nullSafe(item.getHashSha384())).append('|')
                    .append(nullSafe(item.getHashSha256())).append('|')
                    .append(item.getTamanhoBytes()).append('|')
                    .append(nullSafe(item.getStorageUri())).append(';');
        }
        return Hashes.sha256Hex(sb.toString());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record DocumentBatchSnapshot(
            UUID batchId,
            Long processoId,
            Long createdByUserId,
            String status,
            Integer expectedCount,
            int itemCount,
            long uploadedCount,
            long reservedCount,
            long linkedCount,
            long failedCount,
            long totalBytes,
            String fingerprint
    ) {
    }
}
