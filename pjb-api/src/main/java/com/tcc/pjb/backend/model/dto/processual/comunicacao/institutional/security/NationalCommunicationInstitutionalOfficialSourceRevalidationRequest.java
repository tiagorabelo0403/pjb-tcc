package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalOfficialSourceRevalidationRequest(
        List<String> fundamentos
) {
}
