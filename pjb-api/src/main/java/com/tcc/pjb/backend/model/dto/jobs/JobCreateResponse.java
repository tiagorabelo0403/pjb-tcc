package com.tcc.pjb.backend.model.dto.jobs;

import java.util.UUID;

public record JobCreateResponse(
        UUID jobId,
        String status,
        boolean replay,
        boolean inProgress
) {
}
