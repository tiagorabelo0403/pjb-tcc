package com.tcc.pjb.backend.model.dto.admin.backfill;

import java.util.List;

public record AdminMniMigrationEnqueueResponse(List<Long> itemIds) {
}
