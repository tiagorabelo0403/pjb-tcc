package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Documento formal assinado com envelope qualificado Laiane-MP")
public record LaianeDocumentoFormalAssinadoResponse(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "Título do documento processual assinado",
                example = "Requisição de informações — Portaria 123/2026",
                requiredMode = Schema.RequiredMode.REQUIRED) String tituloDocumento,
        @NotBlank
        @Size(max = 500_000)
        @Schema(description = "Conteúdo textual assinado, renderizado na forma definitiva",
                example = "Exmo. Sr. Diretor, solicitamos...",
                requiredMode = Schema.RequiredMode.REQUIRED) String conteudoAssinado,
        @NotBlank
        @Size(min = 64, max = 64)
        @Schema(description = "Hash SHA-256 do conteúdo assinado (64 caracteres hexadecimais)",
                example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                requiredMode = Schema.RequiredMode.REQUIRED) String hashSha256,
        @Schema(description = "Indica se o documento foi selado com cadeia de custódia soberana",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean selado,
        @NotNull
        @Schema(description = "Assinatura qualificada ICP-Brasil vinculada ao documento",
                requiredMode = Schema.RequiredMode.REQUIRED) LaianeAssinaturaQualificadaResponse assinaturaQualificada,
        @NotNull
        @Schema(description = "Resultado da validação soberana da assinatura",
                requiredMode = Schema.RequiredMode.REQUIRED) LaianeValidacaoSoberanaResponse validacaoSoberana
) {
}
