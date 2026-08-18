package com.tcc.pjb.backend.model.dto.shared.reading;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadados do fluxo de leitura processual — capacidades de navegação e preferências de preset")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessReadingFlowMetadataDto(
        @Schema(description = "Cluster de usuário para personalização", example = "MAGISTRATURA")
        String userCluster,
        @Schema(description = "Suporte a leitura inline de atos", example = "true")
        boolean supportsInlineReading,
        @Schema(description = "Suporte a atos nativos no fluxo", example = "true")
        boolean supportsNativeActs,
        @Schema(description = "Suporte a modo cronológico", example = "true")
        boolean supportsChronology,
        @Schema(description = "Suporte a overlay operacional", example = "true")
        boolean supportsOperationalOverlay,
        @Schema(description = "Suporte a atos HTML inline", example = "true")
        boolean supportsInlineHtmlActs,
        @Schema(description = "Suporte a inspeção de PDF assinado", example = "true")
        boolean supportsSignedPdfInspection,
        @Schema(description = "Lane padrão de exibição", example = "ATOS")
        String defaultLane,
        @Schema(description = "Modo de faixa de foco ativa", example = "FOCO_DISCRETO_POR_PECA")
        String focusBandMode,
        @Schema(description = "Modo de cronologia ativa", example = "CRONO_MOVIMENTACOES")
        String chronologyMode,
        @Schema(description = "Endpoint de abertura do fluxo", example = "/api/v1/processos/1/painel-leitura/fluxo")
        String openEndpoint
) {}
