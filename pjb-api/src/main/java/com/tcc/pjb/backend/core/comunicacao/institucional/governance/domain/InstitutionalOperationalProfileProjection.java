package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalOperationalProfileProjection(
        String profileKey,
        String profileState,
        boolean visibleInPjb,
        String affiliationId,
        String nominationId,
        Long nominatedUserId,
        String nominatedUserName,
        String tipoUsuario,
        String organizationScope,
        String destinatarioKind,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String accessLaneKind,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        String panelCode,
        String landingPath,
        String accentColor,
        String processAreaCode,
        String trustFloor,
        boolean activeNomination,
        boolean fullyApproved,
        boolean readyForInstitutionalPanel,
        boolean routeToPersonalPanel,
        boolean directPersonalAccessAvailable,
        boolean localUnitPresent,
        String coverageMode,
        String responsibleTribunalCode,
        String responsibleUnitCode,
        String responsibleUnitName,
        String responsibleComarca,
        String horizontalDataPlaneKey,
        String primaryWritePartitionKey,
        String readReplicaCode,
        List<String> capacidades,
        List<String> requiredApprovals,
        List<String> approvedApprovals,
        List<String> pendingApprovals,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalOperationalProfileProjection {
        Objects.requireNonNull(capacidades);
        Objects.requireNonNull(requiredApprovals);
        Objects.requireNonNull(approvedApprovals);
        Objects.requireNonNull(pendingApprovals);
        Objects.requireNonNull(findings);
        Objects.requireNonNull(fundamentos);
        Objects.requireNonNull(generatedAt);
    }
}
