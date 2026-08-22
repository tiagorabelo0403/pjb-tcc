package com.tcc.pjb.backend.model.dto.admin.backfill;

import com.tcc.pjb.backend.integration.mni.migration.MniMigrationBatchItem;
import java.time.Instant;

public record AdminMniMigrationFailedItemDto(
        Long id,
        String tribunalOrigem,
        String motivo,
        String erro,
        Instant processadoEm
) {
    public static AdminMniMigrationFailedItemDto from(MniMigrationBatchItem item) {
        return new AdminMniMigrationFailedItemDto(
                item.getId(),
                item.getTribunalOrigem(),
                item.getMotivo(),
                item.getErro(),
                item.getProcessadoEm());
    }
}
