package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalProcessAuthorityBandResponse(
        String code,
        String title,
        String accentColor,
        boolean enabled,
        boolean sensitive,
        List<String> allowedActions,
        List<String> prohibitedActions,
        List<String> requiredGuards,
        List<String> fundamentos
) {
}
