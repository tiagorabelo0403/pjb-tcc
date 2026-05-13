package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;

public record DiligenceOperationalClosureRequest(
        Long certidaoId,
        Long checkpointEventId,
        DiligenciaEncerramentoTipo outcome,
        @Size(max = 64) String idempotencyKey,
        @Size(max = 32) String evidenceChaveCustodia,
        @Size(max = 1500) String observacoes,
        List<UUID> documentoIds
) {
}
