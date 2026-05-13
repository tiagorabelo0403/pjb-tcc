package com.tcc.pjb.backend.model.dto.profile;

import jakarta.validation.constraints.Size;

public record DiligenceAutomaticFilingRequest(
        Long formalizacaoId,
        @Size(max = 64) String idempotencyKey,
        Boolean gerarPacotePdf,
        Boolean registrarMovimentacao,
        Boolean exportarMalhaExterna,
        @Size(max = 40) String externalSystemCode,
        @Size(max = 255) String pacoteTitulo,
        @Size(max = 3000) String complementoNarrativo
) {
}
