package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalTrustGovernanceProfileResponse(
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
}
