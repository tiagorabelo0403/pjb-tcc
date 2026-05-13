package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalTextClosureAuditResponse(
        String auditId,
        boolean fullyClosed,
        int totalItems,
        int implementedItems,
        List<NationalCommunicationInstitutionalTextClosureItemResponse> items,
        List<String> fundamentos,
        Instant checkedAt
) {
}
