package com.tcc.pjb.backend.model.dto.consultapublica;

import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultaPublicaProcessoViewResponse(
        String etag,
        LocalDateTime generatedAt,
        int refreshAfterSeconds,
        PublicProcessoResumoCardDto resumo,
        ConsultaPublicaWorkspaceAccessibilityDto accessibility,
        List<ConsultaPublicaWorkspaceActionDto> actions,
        List<String> warnings
) {
}
