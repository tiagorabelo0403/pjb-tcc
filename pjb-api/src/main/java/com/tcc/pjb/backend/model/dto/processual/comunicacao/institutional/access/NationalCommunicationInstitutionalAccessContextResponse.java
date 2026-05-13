package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalCommunicationInstitutionalAccessContextResponse(
        String profileKey,
        String affiliationId,
        String nominationId,
        String panelCode,
        String processAreaCode,
        String primaryUnitCode,
        String primaryBoxCode,
        String coverageMode,
        String horizontalDataPlaneKey,
        String primaryWritePartitionKey,
        String readReplicaCode,
        String trustFloor,
        boolean readyForInstitutionalPanel,
        boolean fullyApproved,
        boolean readOnly,
        boolean requiresStepUp,
        boolean requiresQualifiedCertificate,
        boolean judicialFlowSensitive,
        String rlsScopeKey,
        List<String> allowedUnitCodes,
        List<String> allowedBoxCodes,
        List<String> allowedLaneCodes,
        List<String> activeCoverageDelegationIds,
        List<String> restrictionTags,
        Map<String, String> sessionVariables,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
