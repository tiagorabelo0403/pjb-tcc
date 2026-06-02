package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialFrontendCatalogResponse(
        String menuPrincipal,
        String version,
        String basePath,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        List<String> dominiosSuportados,
        List<CalculoJudicialFrontendDomainResponse> dominios,
        @Schema(description = "Configuracao de interface do catalogo de calculo judicial — varia por configuracao de dominio e perfil institucional", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> ui,
        @Schema(description = "Mapa de mensagens de erro do catalogo — chaves e textos variam por tipo de erro e dominio", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> erros,
        Instant geradoEm
) {
}

