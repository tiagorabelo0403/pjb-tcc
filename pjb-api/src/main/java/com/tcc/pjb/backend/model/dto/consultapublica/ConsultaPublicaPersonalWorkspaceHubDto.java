package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaPersonalWorkspaceHubDto(
        String headline,
        String summary,
        ConsultaPublicaPersonalWorkspaceSummaryDto metrics,
        List<ConsultaPublicaWorkspaceActionDto> quickActions,
        List<ConsultaPublicaPersonalWorkspaceModuleDto> modules,
        List<String> innovations,
        List<String> warnings
) {
}
