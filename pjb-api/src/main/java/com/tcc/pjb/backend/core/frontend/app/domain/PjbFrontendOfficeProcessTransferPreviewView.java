package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeProcessTransferPreviewView(
        Long sourceEquipeId,
        String sourceEquipeNome,
        Long targetEquipeId,
        String targetEquipeNome,
        Long targetResponsibleUserId,
        String targetResponsibleNome,
        int processCount,
        int sensitiveProcessCount,
        boolean valid,
        boolean requiresManualReview,
        String previewSummary,
        String previewHash,
        Integer targetTrustScore,
        String targetTrustLevel,
        Integer targetMinTrustRequired,
        boolean targetCanViewAllRamos,
        List<String> targetAllowedRamos,
        List<String> blockers,
        List<String> warnings,
        List<PjbFrontendOfficeProcessTransferImpactItemView> items
) {
}
