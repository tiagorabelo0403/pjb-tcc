package com.tcc.pjb.backend.model.dto.consultapublica;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultaPublicaWorkspaceResponse(
        String etag,
        LocalDateTime generatedAt,
        String mode,
        String headline,
        String summary,
        ConsultaPublicaSearchConfigDto search,
        ConsultaPublicaWorkspaceRoutesDto routes,
        ConsultaPublicaWorkspaceAccessibilityDto accessibility,
        ConsultaPublicaWorkspaceDatasetDto datasets,
        List<ConsultaPublicaSearchJourneyDto> journeys,
        List<ConsultaPublicaPublicActDto> publicActs,
        List<ConsultaPublicaWorkspaceSectionDto> sections,
        ConsultaPublicaPersonalWorkspaceHubDto personalHub,
        List<ConsultaPublicaPersonalProcessCardDto> meusProcessos,
        List<String> warnings
) {
}
