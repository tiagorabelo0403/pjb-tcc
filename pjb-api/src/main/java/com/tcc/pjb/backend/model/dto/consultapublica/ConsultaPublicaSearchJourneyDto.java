package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaSearchJourneyDto(
        String code,
        String label,
        String description,
        String route,
        String method,
        String inputMode,
        boolean requiresRegionalDisambiguation,
        boolean directProcessList,
        boolean personalContextPreferred,
        List<String> supportedFields
) {
}
