package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeProcessTransferImpactItemView(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String nivelSigilo,
        boolean sensitive,
        boolean blocked,
        boolean requiresManualReview,
        List<String> blockers,
        List<String> warnings
) {
}
