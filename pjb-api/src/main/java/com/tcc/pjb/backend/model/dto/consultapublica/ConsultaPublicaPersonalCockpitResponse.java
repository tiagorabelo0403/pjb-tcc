package com.tcc.pjb.backend.model.dto.consultapublica;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultaPublicaPersonalCockpitResponse(
        LocalDateTime generatedAt,
        Long usuarioId,
        String mode,
        String accessMode,
        String headline,
        ConsultaPublicaWorkspaceRoutesDto routes,
        ConsultaPublicaPersonalWorkspaceSummaryDto portfolio,
        ConsultaPublicaPersonalCalendarDigestDto portfolioCalendar,
        ConsultaPublicaPersonalMovementDigestDto portfolioMovement,
        ConsultaPublicaPersonalCockpitSpotlightDto spotlight,
        List<ConsultaPublicaPersonalCalculatorHintDto> calculatorHints,
        ConsultaPublicaPersonalAiAssistDto aiAssist,
        List<ConsultaPublicaWorkspaceActionDto> quickActions,
        List<String> capabilityFlags,
        List<String> warnings
) {
}
