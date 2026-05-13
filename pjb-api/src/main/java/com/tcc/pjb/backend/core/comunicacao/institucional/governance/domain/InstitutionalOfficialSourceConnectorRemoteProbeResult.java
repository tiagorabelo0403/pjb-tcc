package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.util.List;

public record InstitutionalOfficialSourceConnectorRemoteProbeResult(
        boolean reachable,
        int httpStatus,
        long latencyMillis,
        List<String> signals,
        List<String> blockers,
        List<String> fundamentos
) {
}
