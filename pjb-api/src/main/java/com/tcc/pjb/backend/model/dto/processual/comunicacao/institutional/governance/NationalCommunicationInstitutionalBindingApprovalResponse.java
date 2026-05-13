package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalBindingApprovalResponse(
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String unidadeCodigo,
        String caixaCodigo,
        boolean affiliationActive,
        boolean nominationActive,
        boolean dualAdministrationSatisfied,
        boolean recertificationDue,
        boolean capacityBound,
        boolean homologated,
        boolean approved,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
}
