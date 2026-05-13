package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalDelegatedScopeCoverageResponse(
        String organizationScope,
        String displayName,
        boolean delegatedInstitutionalEntry,
        boolean forumOrJudicialUnit,
        List<String> lanes,
        List<String> guardRails,
        List<String> fundamentos
) {
}
