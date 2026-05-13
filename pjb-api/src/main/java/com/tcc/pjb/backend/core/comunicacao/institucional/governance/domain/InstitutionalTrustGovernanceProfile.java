package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalTrustGovernanceProfile(
        String profileKey,
        String affiliationId,
        String nominationId,
        Long nominatedUserId,
        String nominatedUserName,
        String tipoUsuario,
        String organizationScope,
        String destinatarioKind,
        String unidadeCodigo,
        String caixaCodigo,
        String panelCode,
        String landingPath,
        String accentColor,
        String processAreaCode,
        String trustFloor,
        boolean requiresStepUp,
        boolean requiresCertificate,
        boolean requiresInstitutionalNetwork,
        boolean directPersonalAccessAvailable,
        boolean judicialFlowSensitive,
        List<String> requiredApprovals,
        List<String> approvedApprovals,
        List<String> pendingApprovals,
        boolean fullyApproved,
        boolean readyForInstitutionalPanel,
        boolean routeToPersonalPanel,
        String horizontalDataPlaneKey,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalTrustGovernanceProfile {
        requiredApprovals = requiredApprovals == null ? List.of() : List.copyOf(requiredApprovals);
        approvedApprovals = approvedApprovals == null ? List.of() : List.copyOf(approvedApprovals);
        pendingApprovals = pendingApprovals == null ? List.of() : List.copyOf(pendingApprovals);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
