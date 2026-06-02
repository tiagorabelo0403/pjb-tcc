package com.tcc.pjb.backend.model.dto.shared.calculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Referência ao teto de benefícios previdenciários do INSS vigente")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculoJudicialInssReferenceDto(
        @Schema(description = "Teto do benefício previdenciário do INSS vigente para o exercício de 2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal tetoBeneficio2026,
        @Schema(description = "Data de início da vigência no formato ISO-8601",
                example = "2026-01-01",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String vigenteDesde,
        @Schema(description = "URL da publicação oficial do INSS")
        String fonteOficial,
        @Schema(description = "Regra de uso deste teto nos cálculos judiciais — classificação RPV e precatório")
        String regraUso
) {}
