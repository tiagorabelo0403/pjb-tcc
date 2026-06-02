package com.tcc.pjb.backend.model.dto.shared.calculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Referência ao salário mínimo nacional vigente conforme decreto presidencial")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculoJudicialSalarioMinimoDto(
        @Schema(description = "Valor do salário mínimo nacional vigente conforme decreto presidencial em vigor",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal vigente,
        @Schema(description = "Data de início da vigência no formato ISO-8601",
                example = "2026-01-01",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String vigenteEm,
        @Schema(description = "Valor de referência para o exercício de 2025 conforme decreto vigente naquele período")
        BigDecimal referencia2025,
        @Schema(description = "Valor de referência para o exercício de 2026 conforme decreto vigente naquele período")
        BigDecimal referencia2026,
        @Schema(description = "Norma legal que estabelece o valor vigente",
                example = "Decreto nº 12.797/2025")
        String normaReferencia,
        @Schema(description = "URL da publicação oficial no Portal do Planalto")
        String fonteOficial
) {}
