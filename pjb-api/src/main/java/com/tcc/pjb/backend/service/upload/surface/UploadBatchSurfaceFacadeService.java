package com.tcc.pjb.backend.service.upload.surface;

import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchFinalizePayload;
import com.tcc.pjb.backend.model.dto.upload.UploadIngressResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveResponse;
import com.tcc.pjb.backend.service.upload.BulkUploadIngressService;
import com.tcc.pjb.backend.service.upload.BulkUploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UploadBatchSurfaceFacadeService {

    private final BulkUploadService bulkUploadService;
    private final BulkUploadIngressService ingressService;
    private final JobCommandService jobCommandService;
    private final CurrentUserService currentUserService;

    public UploadBatchSurfaceFacadeService(BulkUploadService bulkUploadService,
                                           BulkUploadIngressService ingressService,
                                           JobCommandService jobCommandService,
                                           CurrentUserService currentUserService) {
        this.bulkUploadService = bulkUploadService;
        this.ingressService = ingressService;
        this.jobCommandService = jobCommandService;
        this.currentUserService = currentUserService;
    }

    public UploadBatchCreateResponse createBatch(UploadBatchCreateRequest req) {
        var batch = bulkUploadService.createBatch(req.processoId(), req.expectedCount());
        return new UploadBatchCreateResponse(batch.getId(), batch.getStatus().name());
    }

    public UploadItemReserveResponse reserveItem(UUID batchId, UploadItemReserveRequest req) {
        return bulkUploadService.reserveItem(batchId, req);
    }

    public UploadIngressResponse directUpload(UUID batchId, UUID itemId, String token, HttpServletRequest request) throws Exception {
        long len = request.getContentLengthLong();
        var result = ingressService.ingest(batchId, itemId, token, len, request.getInputStream());
        return new UploadIngressResponse(result.status(), result.sha256(), result.sha384(), result.storageUri());
    }

    public JobCreateResponse finalizeBatch(UUID batchId, String idempotencyKey, String clientRequestId) {
        String owner = safeOwner();
        String key = firstNonBlank(idempotencyKey, clientRequestId).orElse(UUID.randomUUID().toString());
        String inboxKey = "uploads:batch:" + batchId;
        var result = jobCommandService.createIdempotent(JobType.BULK_FINALIZE_ATTACHMENTS, inboxKey, owner, key, new UploadBatchFinalizePayload(batchId), 0, 5);
        return new JobCreateResponse(result.jobId(), "PENDING", result.replay(), result.inProgress());
    }

    private String safeOwner() {
        try {
            long id = currentUserService.currentUserIdOrZero();
            return id > 0 ? String.valueOf(id) : "anonymous";
        } catch (Exception ignored) {
            return "anonymous";
        }
    }

    private Optional<String> firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return Optional.of(a);
        if (b != null && !b.isBlank()) return Optional.of(b);
        return Optional.empty();
    }
}
