package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialFrontendBootstrapResponse(
        String codigo,
        String slug,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        Map<String, String> rotas,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> http,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> aiAgents,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> officialTables,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payloadInicial,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> iaRequestExemplo,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> requestExemplo,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> responseExemplo,
@Schema(description = "Payload de bootstrap do modulo de calculo judicial — exemplos de request/response gerados dinamicamente por rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> errorExemplo,
        Instant geradoEm
) {
}

