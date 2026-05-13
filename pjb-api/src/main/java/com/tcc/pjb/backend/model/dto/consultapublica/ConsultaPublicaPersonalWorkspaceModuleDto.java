package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaPersonalWorkspaceModuleDto(
        String code,
        String title,
        String summary,
        String route,
        String method,
        String tone,
        boolean processScoped,
        boolean enabled
) {
}
