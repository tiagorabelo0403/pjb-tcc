package com.tcc.pjb.backend.model.dto.shared.reading;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Capacidades e configurações de frontend para leitura do ecossistema processual")
public record ProcessReadingEcosystemFrontendCapabilitiesDto(
        @Schema(description = "Modo de leitura ativo", example = "PROCESSO_CENTRICO_CONVERGENTE")
        String readerMode,
        @Schema(description = "Surface padrão de exibição", example = "HTML_NATIVO")
        String defaultSurface,
        @Schema(description = "Preferência por HTML inline", example = "true")
        boolean preferInlineHtml,
        @Schema(description = "Preferência por inspeção de PDF assinado", example = "false")
        boolean preferSignedPdfInspection,
        @Schema(description = "Suporte a assinatura em nuvem", example = "true")
        boolean supportsCloudSigning,
        @Schema(description = "Suporte a MFA mobile", example = "true")
        boolean supportsMobileMfa,
        @Schema(description = "Suporte a pipeline OCR", example = "true")
        boolean supportsOcrPipeline,
        @Schema(description = "Suporte a agregação nacional de prazos", example = "true")
        boolean supportsNationalDeadlineAggregation,
        @Schema(description = "Suporte a copiloto de IA jurídica", example = "true")
        boolean supportsAiCopilot,
        @Schema(description = "Suporte a malha de migração legada", example = "true")
        boolean supportsLegacyMigrationMesh,
        @Schema(description = "Suporte a acesso nativo pelo browser", example = "true")
        boolean supportsBrowserNativeAccess,
        @Schema(description = "Badges de capacidade ativa do ecossistema")
        List<String> capabilityBadges
) {}
