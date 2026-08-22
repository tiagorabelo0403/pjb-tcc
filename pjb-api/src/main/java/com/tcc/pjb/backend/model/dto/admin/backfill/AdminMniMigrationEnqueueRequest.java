package com.tcc.pjb.backend.model.dto.admin.backfill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminMniMigrationEnqueueRequest(
        @NotEmpty @Valid List<AdminMniMigrationItemRequest> itens
) {
}
