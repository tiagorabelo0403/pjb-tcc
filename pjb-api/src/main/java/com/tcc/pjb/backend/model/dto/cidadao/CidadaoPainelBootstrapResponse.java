package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoPainelBootstrapResponse(
        String etag,
        LocalDateTime generatedAt,
        CidadaoPerfilResumoDto perfil,
        CidadaoPainelBadgesDto badges,
        List<CidadaoPendenciaDto> pendencias,
        List<CidadaoProximoEventoDto> proximosEventos,
        List<CidadaoProcessoCardDto> recentes,
        CidadaoGovHubDto govHub,
        String legendUrl,
        AreaLinks links,
        CidadaoAreaNavLinks navLinks
) {
}
