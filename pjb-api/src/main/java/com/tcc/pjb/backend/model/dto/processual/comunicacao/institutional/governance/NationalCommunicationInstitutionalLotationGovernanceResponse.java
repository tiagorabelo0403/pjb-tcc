package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalLotationGovernanceResponse(
        String lotationId,
        String nominationId,
        Long userId,
        String userName,
        String unitCode,
        String boxCode,
        String laneCode,
        String nominationRole,
        String operationalFunction,
        String trustFloor,
        boolean active,
        Instant activeFrom,
        Instant activeUntil,
        List<String> findings
) {
}
