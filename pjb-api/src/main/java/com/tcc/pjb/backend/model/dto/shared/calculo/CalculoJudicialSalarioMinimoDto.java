package com.tcc.pjb.backend.model.dto.shared.calculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Referência ao salário mínimo nacional vigente conforme decreto presidencial")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculoJudicialSalarioMinimoDto(
        @Schema(description = "Valor do salário mínimo nacional vigente na data da consulta",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal vigente,
        @Schema(description = "Data de início da vigência do valor vigente no formato ISO-8601",
                example = "2026-01-01",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String vigenteEm,
        @Schema(description = "Ano de referência do valor vigente: o último ano com salário mínimo cadastrado até a data da consulta",
                example = "2026")
        Integer anoVigente,
        @Schema(description = "Ano de referência anterior ao do valor vigente", example = "2025")
        Integer anoReferenciaAnterior,
        @Schema(description = "Salário mínimo do ano de referência anterior ao do valor vigente")
        BigDecimal referenciaAnterior,
        @Schema(description = "Norma legal que estabelece o valor vigente",
                example = "Decreto 12.797/2025")
        String normaReferencia,
        @Schema(description = "Fonte oficial da norma: URL da publicação no Planalto quando registrada, senão o órgão que a publicou")
        String fonteOficial
) {}
