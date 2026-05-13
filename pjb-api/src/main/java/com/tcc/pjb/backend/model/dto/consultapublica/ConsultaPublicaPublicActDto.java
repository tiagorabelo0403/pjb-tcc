package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaPublicActDto(
        String code,
        String label,
        String description,
        String routeTemplate,
        boolean textualPreviewEnabled
) {
}
