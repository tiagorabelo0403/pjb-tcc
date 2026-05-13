package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalCoverageDelegationEntryResponse(
        String delegationId,
        String sourceLotationId,
        Long sourceUserId,
        String sourceUserName,
        String targetLotationId,
        Long targetUserId,
        String targetUserName,
        String unitCode,
        String boxCode,
        String laneCode,
        String delegationKind,
        Instant activeFrom,
        Instant activeUntil,
        boolean active,
        boolean crossMunicipalitySupport,
        List<String> findings
) {
}
