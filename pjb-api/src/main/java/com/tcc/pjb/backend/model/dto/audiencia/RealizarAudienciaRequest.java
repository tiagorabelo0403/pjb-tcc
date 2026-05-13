package com.tcc.pjb.backend.model.dto.audiencia;

import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RealizarAudienciaRequest(
        @NotNull StatusAudiencia resultadoStatus,
        @Size(max = 20000) String notas
) {
}
