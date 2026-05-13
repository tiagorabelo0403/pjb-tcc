package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOfficialSourceConnectorResponse(
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
        String connectorStatus,
        boolean connectorEnabled,
        boolean connectorLiveVerificationSupported,
        String connectorReferenceUrl,
        Instant connectorCheckedAt,
        Instant connectorNextCheckAt,
        List<String> connectorSignals,
        List<String> connectorBlockers,
        List<String> fundamentos
) {
}
