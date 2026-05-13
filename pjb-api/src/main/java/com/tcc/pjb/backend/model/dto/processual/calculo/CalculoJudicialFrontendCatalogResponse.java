package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialFrontendCatalogResponse(
        String menuPrincipal,
        String version,
        String basePath,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        List<String> dominiosSuportados,
        List<CalculoJudicialFrontendDomainResponse> dominios,
        Map<String, Object> ui,
        Map<String, Object> erros,
        Instant geradoEm
) {
}
