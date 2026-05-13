package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.util.List;

public record NationalCommunicationInstitutionalAffiliationValidationFindingResponse(
        String code,
        String severity,
        boolean blocking,
        String message,
        List<String> evidences
) {
}
