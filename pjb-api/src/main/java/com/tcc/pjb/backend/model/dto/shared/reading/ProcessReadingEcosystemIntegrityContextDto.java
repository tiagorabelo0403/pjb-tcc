package com.tcc.pjb.backend.model.dto.shared.reading;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contexto de integridade do ecossistema processual — competência, sistemas e cobertura")
public record ProcessReadingEcosystemIntegrityContextDto(
        @Schema(description = "Indica se a competência foi resolvida", example = "true")
        boolean competenceResolved,
        @Schema(description = "Sistema judicial primário", example = "PJE")
        String primarySystem,
        @Schema(description = "Sistema judicial de fallback", example = "PDPJ")
        String fallbackSystem,
        @Schema(description = "Percentual de cobertura textual dos autos (0-100)", example = "85")
        int textCoverage,
        @Schema(description = "Preferência por HTML inline ativa", example = "true")
        boolean htmlInlinePreferred,
        @Schema(description = "Inspeção de PDF assinado obrigatória", example = "false")
        boolean signedPdfInspectionRequired,
        @Schema(description = "Trilha recursal identificada", example = "RECURSAL_ORDINARIO")
        String recursalTrack,
        @Schema(description = "Trilha de embargos identificada", example = "SEM_EMBARGOS")
        String embargoTrack
) {}
