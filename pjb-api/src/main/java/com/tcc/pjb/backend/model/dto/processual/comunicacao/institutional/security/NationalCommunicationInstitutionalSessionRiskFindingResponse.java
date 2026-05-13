package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalSessionRiskFindingResponse(
        String code,
        String severity,
        boolean blocking,
        String message,
        List<String> evidences
) {
}
