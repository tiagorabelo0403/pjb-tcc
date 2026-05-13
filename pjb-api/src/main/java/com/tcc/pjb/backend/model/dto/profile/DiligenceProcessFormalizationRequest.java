package com.tcc.pjb.backend.model.dto.profile;

import jakarta.validation.constraints.Size;

public record DiligenceProcessFormalizationRequest(
        Long encerramentoId,
        Long certidaoId,
        @Size(max = 64) String idempotencyKey,
        Boolean registrarMovimentacao,
        Boolean gerarMinuta,
        @Size(max = 255) String minutaTitulo,
        @Size(max = 3000) String complementoNarrativo,
        @Size(max = 32) String evidenceChaveCustodia
) {
}
