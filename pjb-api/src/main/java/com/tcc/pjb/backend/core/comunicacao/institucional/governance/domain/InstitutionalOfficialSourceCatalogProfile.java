package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.util.List;

public record InstitutionalOfficialSourceCatalogProfile(
        String sourceCode,
        String sourceLabel,
        String authority,
        String authorityScope,
        String accessMode,
        String refreshMode,
        boolean directGovernmentSource,
        boolean autoRefreshSupported,
        int baseConfidence,
        String officialReferenceUrl,
        List<String> defaultSafeSteps,
        List<String> defaultFundamentos
) {
}
