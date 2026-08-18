package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Regra de validação aplicada à assinatura qualificada")
public record LaianeRegraValidacaoResponse(
        @Schema(description = "Código identificador da regra de validação",
                example = "ASSINATURA_QUALIFICADA_COMPLETA",
                requiredMode = Schema.RequiredMode.REQUIRED) String codigo,
        @Schema(description = "Descrição legível da regra aplicada",
                example = "Verifica completude da assinatura qualificada ICP-Brasil") String descricao,
        @Schema(description = "Resultado da aplicação da regra",
                example = "APROVADO",
                allowableValues = {"APROVADO", "REPROVADO", "IGNORADO", "PENDENTE"}) String resultado
) {
}
