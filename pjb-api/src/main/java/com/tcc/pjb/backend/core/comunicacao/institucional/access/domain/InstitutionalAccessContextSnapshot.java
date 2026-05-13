package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record InstitutionalAccessContextSnapshot(
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
    public InstitutionalAccessContextSnapshot {
        allowedUnitCodes = allowedUnitCodes == null ? List.of() : List.copyOf(allowedUnitCodes);
        allowedBoxCodes = allowedBoxCodes == null ? List.of() : List.copyOf(allowedBoxCodes);
        allowedLaneCodes = allowedLaneCodes == null ? List.of() : List.copyOf(allowedLaneCodes);
        activeCoverageDelegationIds = activeCoverageDelegationIds == null ? List.of() : List.copyOf(activeCoverageDelegationIds);
        restrictionTags = restrictionTags == null ? List.of() : List.copyOf(restrictionTags);
        sessionVariables = sessionVariables == null ? Map.of() : Map.copyOf(sessionVariables);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profileKey", profileKey);
        out.put("affiliationId", affiliationId);
        out.put("nominationId", nominationId);
        out.put("panelCode", panelCode);
        out.put("processAreaCode", processAreaCode);
        out.put("primaryUnitCode", primaryUnitCode);
        out.put("primaryBoxCode", primaryBoxCode);
        out.put("coverageMode", coverageMode);
        out.put("horizontalDataPlaneKey", horizontalDataPlaneKey);
        out.put("primaryWritePartitionKey", primaryWritePartitionKey);
        out.put("readReplicaCode", readReplicaCode);
        out.put("trustFloor", trustFloor);
        out.put("readyForInstitutionalPanel", readyForInstitutionalPanel);
        out.put("fullyApproved", fullyApproved);
        out.put("readOnly", readOnly);
        out.put("requiresStepUp", requiresStepUp);
        out.put("requiresQualifiedCertificate", requiresQualifiedCertificate);
        out.put("judicialFlowSensitive", judicialFlowSensitive);
        out.put("rlsScopeKey", rlsScopeKey);
        out.put("allowedUnitCodes", allowedUnitCodes);
        out.put("allowedBoxCodes", allowedBoxCodes);
        out.put("allowedLaneCodes", allowedLaneCodes);
        out.put("activeCoverageDelegationIds", activeCoverageDelegationIds);
        out.put("restrictionTags", restrictionTags);
        out.put("sessionVariables", sessionVariables);
        out.put("findings", findings);
        out.put("fundamentos", fundamentos);
        out.put("generatedAt", generatedAt);
        return Collections.unmodifiableMap(out);
    }
}
