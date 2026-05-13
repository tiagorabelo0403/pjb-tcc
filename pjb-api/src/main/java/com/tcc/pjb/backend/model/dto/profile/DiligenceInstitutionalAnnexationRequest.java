package com.tcc.pjb.backend.model.dto.profile;

import jakarta.validation.constraints.Size;

public record DiligenceInstitutionalAnnexationRequest(
        Long juntadaId,
        @Size(max = 64) String idempotencyKey,
        @Size(max = 40) String externalSystemCode,
        @Size(max = 160) String destinationBox,
        Boolean atualizarDocumentoComoExternalizado,
        Boolean registrarEventoProcessual,
        Boolean exigirJuntadaExportavel,
        @Size(max = 3000) String observacoes
) {
}
