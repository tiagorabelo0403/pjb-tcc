package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOperatingModelResponse(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String destinatarioKind,
        String organizationScope,
        String blueprintCode,
        String entryMode,
        boolean institutionManagedRoles,
        boolean personalRootIdentityRequired,
        boolean magistratesEnterThroughForumAndPersonalAccess,
        String coverageMode,
        NationalCommunicationInstitutionalOperatingCoverageResponse coverageRoute,
        List<NationalCommunicationInstitutionalOperatingSeatResponse> administrativeSeats,
        List<NationalCommunicationInstitutionalOperatingRoleBandResponse> roleBands,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
