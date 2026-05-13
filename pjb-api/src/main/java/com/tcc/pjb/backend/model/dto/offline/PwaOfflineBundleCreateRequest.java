package com.tcc.pjb.backend.model.dto.offline;

import jakarta.validation.constraints.NotNull;

public record PwaOfflineBundleCreateRequest(
        @NotNull Long processoId,
        String escopo,
        String deviceFingerprint
) {
}
