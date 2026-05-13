package com.tcc.pjb.backend.model.dto.profile;

import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;

public record DiligenceAutoCertificateRequest(
        Long checkpointEventId,
        DiligenciaCertidaoTipo tipo,
        @Size(max = 32) String evidenceChaveCustodia,
        @Size(max = 1500) String observacoes
) {
}
