package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace;

import java.util.List;

public record NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String accentColor,
        int totalActions,
        int totalSections,
        int totalAuthorityBands,
        int totalSeparators,
        List<String> tabs,
        List<String> fundamentos
) {
}
