package com.tcc.pjb.backend.model.dto.admin.backfill;

import jakarta.validation.constraints.NotBlank;

public record AdminMniMigrationItemRequest(
        String tribunalOrigem,
        String motivo,
        @NotBlank String xml
) {
}
