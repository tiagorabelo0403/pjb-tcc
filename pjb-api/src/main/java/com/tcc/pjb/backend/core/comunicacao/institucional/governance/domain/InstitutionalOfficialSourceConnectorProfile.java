package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceConnectorProfile(
        String sourceCode,
        boolean enabled,
        String connectorStatus,
        boolean liveVerificationSupported,
        String referenceUrl,
        Instant checkedAt,
        Instant nextCheckAt,
        List<String> signals,
        List<String> blockers,
        List<String> fundamentos
) {
}
