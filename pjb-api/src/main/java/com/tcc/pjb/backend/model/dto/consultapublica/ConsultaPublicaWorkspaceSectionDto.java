package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaWorkspaceSectionDto(
        String code,
        String title,
        String description,
        String tone,
        String route,
        boolean enabled
) {
}
