package com.tcc.pjb.backend.model.dto.publico;

import java.util.List;

public record TimelinePublicaDto(
        String numeroProcesso,
        String descricaoSimples,
        List<AtoTimelineDto> atos,
        AtoTimelineDto proximoPasso
) {
}
