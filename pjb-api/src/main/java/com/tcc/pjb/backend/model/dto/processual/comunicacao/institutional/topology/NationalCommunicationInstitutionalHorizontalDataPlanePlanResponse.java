package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse(
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
}
