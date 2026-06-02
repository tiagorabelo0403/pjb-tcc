package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialFrontendDomainResponse(
        String codigo,
        String slug,
        String aba,
        String titulo,
        String descricao,
        Map<String, String> rotas,
        @Schema(description = "Secoes do formulario dinamico de calculo — schema definido pelo rito processual aplicavel", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Map<String, Object>> secoes,
        @Schema(description = "Campos do formulario dinamico de calculo — tipos e validacoes definidos pelo rito processual", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Map<String, Object>> campos,
        @Schema(description = "Resultado do calculo financeiro — estrutura varia por rito (trabalhista/civil/previdenciario/fiscal)", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> resultado,
        @Schema(description = "Configuracao de UX e integracao do dominio de calculo — varia por rito e perfil institucional", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> ux,
        @Schema(description = "Mapa de erros do dominio de calculo — chaves e textos variam por tipo de validacao e rito", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> erros,
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
        Map<String, Object> errorExemplo
) {
}

