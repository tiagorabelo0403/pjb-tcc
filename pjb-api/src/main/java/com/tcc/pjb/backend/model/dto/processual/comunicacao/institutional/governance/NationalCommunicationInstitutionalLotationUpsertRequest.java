package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalLotationUpsertRequest(
        String nominationId,
        Long userId,
        String userName,
        String unitCode,
        String boxCode,
        String laneCode,
        String nominationRole,
        String operationalFunction,
        String trustFloor,
        Boolean active,
        Instant activeFrom,
        Instant activeUntil,
        List<String> fundamentos
) {
}
