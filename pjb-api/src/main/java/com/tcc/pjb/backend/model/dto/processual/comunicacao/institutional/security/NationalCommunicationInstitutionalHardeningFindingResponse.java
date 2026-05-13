package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalHardeningFindingResponse(
        String code,
        String severity,
        String message,
        List<String> evidencias
) {
}
