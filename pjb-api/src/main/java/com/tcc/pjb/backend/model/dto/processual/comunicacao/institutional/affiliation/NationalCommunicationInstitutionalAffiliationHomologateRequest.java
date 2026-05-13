package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.util.List;

public record NationalCommunicationInstitutionalAffiliationHomologateRequest(
        boolean homologar,
        List<String> fundamentos
) {
}
