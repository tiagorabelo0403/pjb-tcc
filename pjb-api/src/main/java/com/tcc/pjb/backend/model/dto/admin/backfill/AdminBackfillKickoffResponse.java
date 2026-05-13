package com.tcc.pjb.backend.model.dto.admin.backfill;

import java.util.UUID;

public record AdminBackfillKickoffResponse(
        UUID jobId,
        String status,
        boolean replay,
        boolean inProgress
) {
}
