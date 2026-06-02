package com.tcc.pjb.backend.model.dto.shared.reading;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Integridade do workspace de leitura — capacidades de superfície disponíveis para o processo")
public record ProcessReadingWorkspaceIntegrityDto(
        @Schema(description = "Processo tem páginas indexadas", example = "true")
        boolean hasPages,
        @Schema(description = "Cobertura textual saudável (>= 65%)", example = "true")
        boolean textCoverageHealthy,
        @Schema(description = "Suporte a leitura em modo foco", example = "true")
        boolean supportsFocusReading,
        @Schema(description = "Suporte a tema âmbar jurídico", example = "true")
        boolean supportsAmberMode,
        @Schema(description = "Suporte a navegação por chunk de páginas", example = "false")
        boolean supportsChunkNavigation,
        @Schema(description = "Suporte a busca de documentos", example = "true")
        boolean supportsDocumentSearch,
        @Schema(description = "Suporte a preset institucional", example = "true")
        boolean supportsInstitutionalPreset,
        @Schema(description = "Suporte a mapa de navegação lateral", example = "true")
        boolean supportsNavigationMap,
        @Schema(description = "Suporte a atos nativos no fluxo", example = "true")
        boolean supportsNativeActs,
        @Schema(description = "Suporte a decisões inline no fluxo", example = "false")
        boolean supportsInlineDecisions,
        @Schema(description = "Suporte a véu de privacidade (sigilo)", example = "false")
        boolean supportsPrivacyVeil,
        @Schema(description = "Suporte a bias de teclado para acessibilidade", example = "true")
        boolean supportsKeyboardBias,
        @Schema(description = "Suporte a malha de contexto procedimental", example = "true")
        boolean supportsProceduralContextMesh,
        @Schema(description = "Suporte a todos os ritos processuais brasileiros", example = "true")
        boolean supportsAllBrazilianRites,
        @Schema(description = "Suporte a todos os direitos processuais brasileiros", example = "true")
        boolean supportsAllBrazilianRights,
        @Schema(description = "Suporte a todas as garantias processuais", example = "true")
        boolean supportsAllProceduralGuarantees,
        @Schema(description = "Suporte a atos HTML inline no fluxo", example = "true")
        boolean supportsInlineHtmlActs,
        @Schema(description = "Suporte a especialização de leitura por rito", example = "true")
        boolean supportsReadingSpecialization,
        @Schema(description = "Suporte a todas as instâncias e embargos", example = "true")
        boolean supportsAllInstancesAndEmbargos,
        @Schema(description = "Suporte a sequência de abertura do processo", example = "false")
        boolean supportsOpeningSequence,
        @Schema(description = "Suporte a inspeção de PDF assinado", example = "false")
        boolean supportsSignedPdfInspection
) {}
