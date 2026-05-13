package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoPainelWidgetsResponse(
        String etag,
        LocalDateTime generatedAt,
        List<CidadaoWidgetDto> widgets
) {
}
