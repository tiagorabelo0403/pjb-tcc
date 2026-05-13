package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalHorizontalDataPlanePlan(
        String profileKey,
        String affiliationId,
        String nominationId,
        String organizationScope,
        String destinatarioKind,
        String requestedMunicipality,
        String requestedUf,
        String responsibleTribunalCode,
        String responsibleUnitCode,
        String responsibleUnitName,
        String responsibleComarca,
        String caixaCodigo,
        String panelCode,
        String landingPath,
        boolean readyForInstitutionalPanel,
        boolean routeToPersonalPanel,
        boolean localUnitPresent,
        String coverageMode,
        String horizontalDataPlaneKey,
        String primaryWritePartitionKey,
        String readReplicaCode,
        int writeShardBucket,
        int writeShardBucketCount,
        String warmArchivePartitionKey,
        List<String> partitionAxes,
        Map<String, String> routingHeaders,
        List<String> requiredApprovals,
        List<String> approvedApprovals,
        List<String> pendingApprovals,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalHorizontalDataPlanePlan {
        partitionAxes = partitionAxes == null ? List.of() : List.copyOf(partitionAxes);
        routingHeaders = routingHeaders == null ? Map.of() : Map.copyOf(routingHeaders);
        requiredApprovals = requiredApprovals == null ? List.of() : List.copyOf(requiredApprovals);
        approvedApprovals = approvedApprovals == null ? List.of() : List.copyOf(approvedApprovals);
        pendingApprovals = pendingApprovals == null ? List.of() : List.copyOf(pendingApprovals);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
