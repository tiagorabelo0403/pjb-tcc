package com.tcc.pjb.backend.modules.laiane.dto.protocol;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da verificação de protocolo de petição no Laiane")
public record LaianeProtocolVerificationResponse(
        @Schema(description = "Indica se o protocolo foi verificado com sucesso",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean verified
) {
}
