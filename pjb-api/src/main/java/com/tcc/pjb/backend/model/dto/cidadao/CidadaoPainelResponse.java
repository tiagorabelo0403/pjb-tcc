package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoPainelResponse(
        LocalDateTime generatedAt,
        CidadaoPerfilResumoDto perfil,
        CidadaoPainelBadgesDto badges,
        List<CidadaoWidgetDto> widgets,
        List<CidadaoPendenciaDto> pendencias,
        List<CidadaoProximoEventoDto> proximosEventos,
        List<CidadaoProcessoCardDto> recentes,
        CidadaoGovHubDto govHub,
        String uiLegendUrl,
        AreaLinks links,
        CidadaoAreaNavLinks nav
) {
}
