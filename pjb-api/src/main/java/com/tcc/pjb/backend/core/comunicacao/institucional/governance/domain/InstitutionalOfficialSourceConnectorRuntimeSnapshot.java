package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceConnectorRuntimeSnapshot(
        String sourceCode,
        String connectorStatus,
        boolean liveVerificationSupported,
        Instant checkedAt,
        Instant nextCheckAt,
        List<String> signals,
        List<String> blockers,
        List<String> fundamentos
) {
}
