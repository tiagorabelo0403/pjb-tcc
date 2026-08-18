package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Resultado da validação soberana de assinatura qualificada Laiane-MP")
public record LaianeValidacaoSoberanaResponse(
        @Schema(description = "Status da validação soberana", example = "APROVADA",
                requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(description = "Fonte da validação (autoridade certificadora ou componente interno)",
                example = "ICP-BRASIL") String fonte,
        @Schema(description = "Política de assinatura aplicada",
                example = "PA_QUALIFICADA_SOBERANA_v2") String politicaAssinatura,
        @Schema(description = "Indica se a cadeia de custódia é elegível",
                example = "true") boolean cadeiaCustodiaElegivel,
        @Schema(description = "Indica se a assinatura completa foi materializada",
                example = "true") boolean assinaturaCompletaMaterializada,
        @Schema(description = "Indica se rubrica, data e hora local estão presentes",
                example = "true") boolean rubricaDataHoraLocalPresentes,
        @Schema(description = "Indica se a classificação contextual é coerente com o conteúdo",
                example = "true") boolean classificacaoContextualCoerente,
        @Schema(description = "Indica se o certificado de entrada está vinculado",
                example = "true") boolean certificadoEntradaVinculado,
        @Schema(description = "Papel detalhado do assinante conforme política institucional",
                example = "PROMOTOR_JUSTIFICA_ESTADUAL") String papelAssinanteDetalhado,
        @Schema(description = "Ramo de Justiça do signatário",
                example = "MINISTERIO_PUBLICO") String ramoJustica,
        @Schema(description = "Instância do signatário (1º grau, 2º grau, superior)",
                example = "1G") String instancia,
        @Schema(description = "Lotação institucional do assinante",
                example = "2ª Promotoria de Justiça Criminal — Comarca de Fortaleza") String lotacaoAssinante,
        @Schema(description = "Hash de vinculação de sessão para anti-replay",
                example = "a3f7c2d1e4b8...") String sessionBindingHash,
        @Schema(description = "Hash de proteção contra replay da assinatura",
                example = "b9e1d3f5a7c2...") String replayShieldHash,
        @Schema(description = "Hash SHA-256 do documento assinado",
                example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855") String documentoAssinadoHash,
        @Size(max = 100)
        @Schema(description = "Lista de regras de validação aplicadas à assinatura (máx. 100 por política ICP-Brasil)",
                maxLength = 100) List<LaianeRegraValidacaoResponse> regrasAplicadas
) {
}
