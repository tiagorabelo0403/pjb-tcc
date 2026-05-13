package com.tcc.pjb.backend.model.dto.ui.frontend;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FrontendOfficeProcessTransferRequest(
        @NotNull Long sourceEquipeId,
        @NotNull Long targetEquipeId,
        @NotNull Long targetResponsibleUserId,
        @NotEmpty List<Long> processoIds,
        String motivo,
        String escopo,
        String idempotencyKey,
        String previewHash
) {
}
