package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Assinatura qualificada ICP-Brasil vinculada a documento formal Laiane-MP")
public record LaianeAssinaturaQualificadaResponse(
        @Schema(description = "Identificador único do envelope de assinatura",
                example = "PJB-ENV-2026-001", requiredMode = Schema.RequiredMode.REQUIRED) String envelopeId,
        @Schema(description = "Hash da assinatura digital",
                example = "a3f7c2...") String assinaturaHash,
        @Schema(description = "Hash base do conteúdo antes da assinatura",
                example = "b9e1d3...") String conteudoBaseHash,
        @Schema(description = "Hash SHA-256 do documento assinado",
                example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855") String documentoAssinadoHash,
        @Schema(description = "Indica se o documento foi rubricado pelo signatário",
                example = "true") boolean rubrica,
        @NotNull
        @Schema(description = "Data da assinatura (LocalDate — sem timezone, data de ato processual)",
                example = "2026-05-31", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate data,
        @NotNull
        @Schema(description = "Hora da assinatura (LocalTime — sem timezone, hora do ato processual)",
                example = "15:30:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalTime hora,
        @Schema(description = "Local físico ou eletrônico da assinatura",
                example = "Quixadá/CE") String local,
        @Schema(description = "Nome do signatário",
                example = "Fulano de Tal") String signatario,
        @Schema(description = "Papel do assinante (classificação primária)",
                example = "MINISTERIO_PUBLICO") String papelAssinante,
        @Schema(description = "Papel detalhado do assinante conforme política institucional",
                example = "PROMOTOR_JUSTICA_ESTADUAL") String papelAssinanteDetalhado,
        @Schema(description = "Segmento institucional do signatário",
                example = "ESTADUAL") String segmentoInstitucional,
        @Schema(description = "Ramo de Justiça do signatário",
                example = "MINISTERIO_PUBLICO") String ramoJustica,
        @Schema(description = "Esfera institucional (federal, estadual, municipal)",
                example = "ESTADUAL") String esferaInstitucional,
        @Schema(description = "Instância do signatário",
                example = "PRIMEIRO_GRAU") String instancia,
        @Schema(description = "Órgão assinante",
                example = "MP/CE") String orgaoAssinante,
        @Schema(description = "Lotação institucional do assinante",
                example = "2ª Promotoria de Justiça Criminal — Comarca de Fortaleza") String lotacaoAssinante,
        @Schema(description = "Registro profissional do signatário (OAB, etc.)",
                example = "MP-CE-4567") String registroProfissional,
        @Schema(description = "Indica coerência entre certificado digital e usuário autenticado",
                example = "true") boolean coerenciaCertificadoUsuario,
        @Schema(description = "Hash de vinculação de sessão para controle anti-replay",
                example = "session-hash-xyz") String sessionBindingHash,
        @Schema(description = "Hash de proteção contra replay da assinatura",
                example = "replay-hash-abc") String replayShieldHash,
        @NotNull
        @Schema(description = "Resultado da validação soberana desta assinatura",
                requiredMode = Schema.RequiredMode.REQUIRED) LaianeValidacaoSoberanaResponse validacaoSoberana
) {
}
