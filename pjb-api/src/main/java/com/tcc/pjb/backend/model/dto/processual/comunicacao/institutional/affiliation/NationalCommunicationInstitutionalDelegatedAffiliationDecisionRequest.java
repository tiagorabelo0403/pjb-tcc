package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.util.List;

public record NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest(
        boolean aprovar,
        List<String> fundamentos
) {
}
