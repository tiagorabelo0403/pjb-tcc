package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalOperatingSeatResponse(
        String code,
        String displayName,
        String laneKind,
        String nominationRole,
        String processProfile,
        String trustFloor,
        boolean managementSeat,
        boolean requiresStepUp,
        boolean requiresCertificate,
        boolean remoteAuthorized,
        List<String> capacities,
        List<String> restrictions,
        List<String> fundamentos
) {
}
