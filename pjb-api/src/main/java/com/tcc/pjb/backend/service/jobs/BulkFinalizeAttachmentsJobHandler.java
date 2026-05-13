package com.tcc.pjb.backend.service.jobs;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;
import com.tcc.pjb.backend.service.upload.BulkUploadService;

@Component
public class BulkFinalizeAttachmentsJobHandler implements JobHandler {

    private final BulkUploadService bulkUploadService;

    public BulkFinalizeAttachmentsJobHandler(BulkUploadService bulkUploadService) {
        this.bulkUploadService = Objects.requireNonNull(bulkUploadService);
    }

    @Override
    public JobType type() {
        return JobType.BULK_FINALIZE_ATTACHMENTS;
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        BulkFinalizeAttachmentsInput input = ctx.inputAs(BulkFinalizeAttachmentsInput.class);
        bulkUploadService.finalizeBatchAsJob(input.batchId(), ctx);
    }

    public record BulkFinalizeAttachmentsInput(UUID batchId) {
    }
}
