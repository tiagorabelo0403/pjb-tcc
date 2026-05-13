package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendOfficeProcessTransferView(
        Long transferId,
        Long sourceEquipeId,
        String sourceEquipeNome,
        Long targetEquipeId,
        String targetEquipeNome,
        Long targetResponsibleUserId,
        String targetResponsibleNome,
        String status,
        int processCount,
        int sensitiveProcessCount,
        String motivo,
        String escopo,
        List<Long> processoIds,
        String previewSummary,
        boolean manageableByCurrentUser,
        boolean actionableByCurrentUser,
        Instant createdAt,
        Instant respondedAt,
        Instant executedAt
) {
}
